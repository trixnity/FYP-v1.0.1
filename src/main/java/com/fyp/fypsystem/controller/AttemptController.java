package com.fyp.fypsystem.controller;

import com.fyp.fypsystem.model.Attempt;
import com.fyp.fypsystem.model.User;
import com.fyp.fypsystem.repository.AttemptRepository;
import com.fyp.fypsystem.repository.PuzzleRepository;
import com.fyp.fypsystem.repository.UserRepository;
import com.fyp.fypsystem.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/attempts")
public class AttemptController {

    private final AttemptRepository attemptRepository;
    private final UserRepository userRepository;
    private final PuzzleRepository puzzleRepository;
    private final JwtUtil jwtUtil;

    public AttemptController(AttemptRepository attemptRepository,
                              UserRepository userRepository,
                              PuzzleRepository puzzleRepository,
                              JwtUtil jwtUtil) {
        this.attemptRepository = attemptRepository;
        this.userRepository = userRepository;
        this.puzzleRepository = puzzleRepository;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping
    public Attempt create(@RequestBody Attempt payload) {
        if (payload.getSolvedAt() == null && payload.isSolved()) {
            payload.setSolvedAt(LocalDateTime.now());
        }
        return attemptRepository.save(payload);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats(@RequestParam(required = false) Long userId,
                                                     @RequestParam(required = false) String email) {
        List<Attempt> attempts = (userId != null)
                ? attemptRepository.findByUserId(userId)
                : attemptRepository.findByUserEmail(email);
        Map<String, Map<String, Integer>> byTheme = new HashMap<>();
        for (Attempt a : attempts) {
            String theme = a.getTheme() != null ? a.getTheme() : "unknown";
            byTheme.putIfAbsent(theme, new HashMap<>());
            Map<String, Integer> stats = byTheme.get(theme);
            stats.put("total", stats.getOrDefault("total", 0) + 1);
            if (a.isSolved()) stats.put("solved", stats.getOrDefault("solved", 0) + 1);
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("byTheme", byTheme);
        resp.put("total", attempts.size());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/wrong-move")
    public ResponseEntity<?> wrongMove(
            @RequestHeader("Authorization") String authHeader) {

        User user = resolveUser(authHeader);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

        int before = user.getRating() != null ? user.getRating() : 1200;
        int after  = Math.max(100, before - 8);
        user.setRating(after);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("ratingBefore", before, "ratingAfter", after, "ratingDelta", after - before));
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submit(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> body) {

        User user = resolveUser(authHeader);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

        Long puzzleId = toLong(body.get("puzzleId"));
        boolean solved = Boolean.TRUE.equals(body.get("solved"));
        int wrongMoves = body.get("wrongMoves") instanceof Number n ? n.intValue() : 0;
        int attemptCount = body.get("attempts") instanceof Number n ? Math.max(1, n.intValue()) : wrongMoves + 1;
        Integer timeSeconds = body.get("timeSeconds") instanceof Number n ? Math.max(0, n.intValue()) : null;
        String theme = body.get("theme") instanceof String s ? s : null;
        boolean firstAttemptSolved = solved && attemptCount == 1;
        int performanceScore = calculatePerformanceScore(solved, attemptCount, timeSeconds);

        // Puzzle difficulty (default 1200 if unknown)
        int puzzleDifficulty = 1200;
        if (puzzleId != null) {
            puzzleDifficulty = puzzleRepository.findById(puzzleId)
                    .map(p -> p.getDifficulty() != null ? p.getDifficulty() : 1200)
                    .orElse(1200);
        }

        int userRating = user.getRating() != null ? user.getRating() : 1200;

        // Elo-style rating delta
        double expected = 1.0 / (1.0 + Math.pow(10.0, (puzzleDifficulty - userRating) / 400.0));
        double actual = solved ? performanceScore / 100.0 : 0.0;
        int delta = (int) Math.round(32 * (actual - expected));
        int newRating = Math.max(100, userRating + delta);

        // Stars: 3 for no mistakes, 2 for some, 1 for solved with many, 0 if not solved
        int stars = solved ? (performanceScore >= 90 ? 3 : performanceScore >= 70 ? 2 : 1) : 0;

        // Save attempt
        Attempt attempt = new Attempt(
                puzzleId, user.getId(), user.getEmail(),
                solved, stars, attemptCount, theme, solved ? LocalDateTime.now() : null);
        attempt.setTimeSeconds(timeSeconds);
        attempt.setPerformanceScore(performanceScore);
        attempt.setFirstAttemptSolved(firstAttemptSolved);
        attemptRepository.save(attempt);

        // Update rating
        user.setRating(newRating);
        userRepository.save(user);

        Map<String, Object> result = new HashMap<>();
        result.put("solved", solved);
        result.put("ratingBefore", userRating);
        result.put("ratingAfter", newRating);
        result.put("ratingDelta", delta);
        result.put("stars", stars);
        result.put("attempts", attemptCount);
        result.put("timeSeconds", timeSeconds);
        result.put("performanceScore", performanceScore);
        result.put("firstAttemptSolved", firstAttemptSolved);
        result.put("masteryLabel", masteryLabel(performanceScore));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/my-progress")
    public ResponseEntity<?> myProgress(@RequestHeader("Authorization") String authHeader) {
        User user = resolveUser(authHeader);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

        List<Attempt> attempts = attemptRepository.findByUserId(user.getId());

        long totalAttempted = attempts.size();
        long totalSolved = attempts.stream().filter(Attempt::isSolved).count();
        int accuracyPct = totalAttempted == 0 ? 0 : (int) ((totalSolved * 100) / totalAttempted);
        int performanceAccuracyPct = attempts.isEmpty() ? 0 : (int) Math.round(attempts.stream()
                .mapToInt(a -> a.getPerformanceScore() != null ? a.getPerformanceScore() : legacyPerformanceScore(a))
                .average()
                .orElse(0));
        double avgAttempts = attempts.isEmpty() ? 0 : attempts.stream()
                .mapToInt(a -> Math.max(1, a.getAttempts()))
                .average()
                .orElse(0);
        double avgTimeSeconds = attempts.stream()
                .filter(a -> a.getTimeSeconds() != null)
                .mapToInt(Attempt::getTimeSeconds)
                .average()
                .orElse(0);

        // Daily activity for last 14 days
        LocalDate today = LocalDate.now();
        LinkedHashMap<LocalDate, long[]> dailyMap = new LinkedHashMap<>();
        for (int i = 13; i >= 0; i--) {
            dailyMap.put(today.minusDays(i), new long[]{0, 0}); // [solved, failed]
        }
        for (Attempt a : attempts) {
            if (a.getSolvedAt() == null) continue;
            LocalDate date = a.getSolvedAt().toLocalDate();
            long[] bucket = dailyMap.get(date);
            if (bucket != null) {
                if (a.isSolved()) bucket[0]++; else bucket[1]++;
            }
        }
        List<Map<String, Object>> dailyActivity = dailyMap.entrySet().stream()
                .map(e -> {
                    Map<String, Object> day = new HashMap<>();
                    day.put("date", e.getKey().toString());
                    day.put("solved", e.getValue()[0]);
                    day.put("failed", e.getValue()[1]);
                    return day;
                })
                .collect(Collectors.toList());

        // Theme breakdown
        Map<String, ThemeStats> themeMap = new LinkedHashMap<>();
        for (Attempt a : attempts) {
            String theme = a.getTheme() != null ? a.getTheme() : "unknown";
            ThemeStats stats = themeMap.computeIfAbsent(theme, key -> new ThemeStats());
            stats.total++;
            if (a.isSolved()) stats.solved++;
            stats.totalAttempts += Math.max(1, a.getAttempts());
            if (a.getTimeSeconds() != null) {
                stats.totalTimeSeconds += a.getTimeSeconds();
                stats.timedCount++;
            }
            stats.totalPerformanceScore += a.getPerformanceScore() != null ? a.getPerformanceScore() : legacyPerformanceScore(a);
        }
        List<Map<String, Object>> themeBreakdown = themeMap.entrySet().stream()
                .map(e -> {
                    ThemeStats stats = e.getValue();
                    int performancePct = stats.total == 0 ? 0 : (int) Math.round((double) stats.totalPerformanceScore / stats.total);
                    Map<String, Object> t = new HashMap<>();
                    t.put("theme", e.getKey());
                    t.put("total", stats.total);
                    t.put("solved", stats.solved);
                    t.put("accuracyPct", stats.total == 0 ? 0 : (int) ((stats.solved * 100) / stats.total));
                    t.put("performanceAccuracyPct", performancePct);
                    t.put("avgAttempts", stats.total == 0 ? 0 : round1((double) stats.totalAttempts / stats.total));
                    t.put("avgTimeSeconds", stats.timedCount == 0 ? null : round1((double) stats.totalTimeSeconds / stats.timedCount));
                    t.put("masteryLabel", masteryLabel(performancePct));
                    return t;
                })
                .sorted((a, b) -> Long.compare(((Number) b.get("total")).longValue(), ((Number) a.get("total")).longValue()))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("totalAttempted", totalAttempted);
        result.put("totalSolved", totalSolved);
        result.put("accuracyPct", accuracyPct);
        result.put("performanceAccuracyPct", performanceAccuracyPct);
        result.put("avgAttempts", round1(avgAttempts));
        result.put("avgTimeSeconds", avgTimeSeconds == 0 ? null : round1(avgTimeSeconds));
        result.put("masteryLabel", masteryLabel(performanceAccuracyPct));
        result.put("dailyActivity", dailyActivity);
        result.put("themeBreakdown", themeBreakdown);

        return ResponseEntity.ok(result);
    }

    private User resolveUser(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        try {
            String email = jwtUtil.extractEmail(authHeader.substring(7));
            return userRepository.findByEmail(email).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        try { return Long.parseLong(val.toString()); }
        catch (NumberFormatException e) { return null; }
    }

    private int calculatePerformanceScore(boolean solved, int attempts, Integer timeSeconds) {
        if (!solved) return 0;
        int attemptScore = Math.max(25, 100 - ((Math.max(1, attempts) - 1) * 25));
        double timeFactor = timeFactor(timeSeconds);
        return Math.max(0, Math.min(100, (int) Math.round(attemptScore * timeFactor)));
    }

    private int legacyPerformanceScore(Attempt attempt) {
        return calculatePerformanceScore(attempt.isSolved(), Math.max(1, attempt.getAttempts()), attempt.getTimeSeconds());
    }

    private double timeFactor(Integer timeSeconds) {
        if (timeSeconds == null) return 1.0;
        if (timeSeconds <= 10) return 1.0;
        if (timeSeconds <= 30) return 0.9;
        if (timeSeconds <= 60) return 0.75;
        if (timeSeconds <= 120) return 0.6;
        return 0.45;
    }

    private String masteryLabel(int score) {
        if (score >= 90) return "Mastered";
        if (score >= 70) return "Strong";
        if (score >= 50) return "Developing";
        return "Weak";
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static class ThemeStats {
        long total;
        long solved;
        long totalAttempts;
        long totalTimeSeconds;
        long timedCount;
        long totalPerformanceScore;
    }
}
