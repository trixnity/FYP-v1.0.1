package com.fyp.fypsystem.controller;

import com.fyp.fypsystem.model.Attempt;
import com.fyp.fypsystem.repository.AttemptRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attempts")
public class AttemptController {
    private final AttemptRepository attemptRepository;

    public AttemptController(AttemptRepository attemptRepository) {
        this.attemptRepository = attemptRepository;
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
}
