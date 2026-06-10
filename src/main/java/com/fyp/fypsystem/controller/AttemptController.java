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
        String theme = body.get("theme") instanceof String s ? s : null;

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
        double actual = solved ? (wrongMoves == 0 ? 1.0 : 0.6) : 0.0;
        int delta = (int) Math.round(32 * (actual - expected));
        int newRating = Math.max(100, userRating + delta);

        // Stars: 3 for no mistakes, 2 for some, 1 for solved with many, 0 if not solved
        int stars = solved ? (wrongMoves == 0 ? 3 : wrongMoves <= 2 ? 2 : 1) : 0;

        // Save attempt
        Attempt attempt = new Attempt(
                puzzleId, user.getId(), user.getEmail(),
                solved, stars, wrongMoves + 1, theme, solved ? LocalDateTime.now() : null);
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
        Map<String, long[]> themeMap = new LinkedHashMap<>();
        for (Attempt a : attempts) {
            String theme = a.getTheme() != null ? a.getTheme() : "unknown";
            themeMap.putIfAbsent(theme, new long[]{0, 0});
            themeMap.get(theme)[0]++;
            if (a.isSolved()) themeMap.get(theme)[1]++;
        }
        List<Map<String, Object>> themeBreakdown = themeMap.entrySet().stream()
                .map(e -> {
                    Map<String, Object> t = new HashMap<>();
                    t.put("theme", e.getKey());
                    t.put("total", e.getValue()[0]);
                    t.put("solved", e.getValue()[1]);
                    t.put("accuracyPct", e.getValue()[0] == 0 ? 0
                            : (int) ((e.getValue()[1] * 100) / e.getValue()[0]));
                    return t;
                })
                .sorted((a, b) -> Long.compare((long) b.get("total"), (long) a.get("total")))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("totalAttempted", totalAttempted);
        result.put("totalSolved", totalSolved);
        result.put("accuracyPct", accuracyPct);
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
}
