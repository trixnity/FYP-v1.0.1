package com.fyp.fypsystem.controller;

import com.fyp.fypsystem.model.ClassApplication;
import com.fyp.fypsystem.model.User;
import com.fyp.fypsystem.security.JwtUtil;
import com.fyp.fypsystem.service.ClassApplicationService;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
public class ClassApplicationController {

    private final ClassApplicationService service;
    private final JwtUtil jwtUtil;

    public ClassApplicationController(ClassApplicationService service, JwtUtil jwtUtil) {
        this.service = service;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/api/coaches")
    public ResponseEntity<List<Map<String, Object>>> coaches() {
        return ResponseEntity.ok(service.coaches().stream().map(this::coachCard).toList());
    }

    @PostMapping("/api/class-applications")
    public ResponseEntity<?> submit(@RequestBody ClassApplication payload,
                                    @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            return ResponseEntity.ok(service.submit(payload, email(authHeader)));
        } catch (AccessDeniedException ex) {
            return unauthorizedOrForbidden(ex);
        }
    }

    @GetMapping("/api/class-applications/my")
    public ResponseEntity<?> mine(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            return ResponseEntity.ok(service.mine(email(authHeader)));
        } catch (AccessDeniedException ex) {
            return unauthorizedOrForbidden(ex);
        }
    }

    @GetMapping("/api/admin/class-applications")
    public ResponseEntity<?> adminList(@RequestParam(value = "status", required = false) String status,
                                       @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            ClassApplication.ApplicationStatus parsed = null;
            if (status != null && !status.isBlank()) {
                parsed = ClassApplication.ApplicationStatus.valueOf(status.trim().toUpperCase());
            }
            return ResponseEntity.ok(service.allForAdmin(email(authHeader), parsed));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status"));
        } catch (AccessDeniedException ex) {
            return unauthorizedOrForbidden(ex);
        }
    }

    @GetMapping("/api/coach/class-applications")
    public ResponseEntity<?> coachAssigned(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            return ResponseEntity.ok(service.assignedToCoach(email(authHeader)));
        } catch (AccessDeniedException ex) {
            return unauthorizedOrForbidden(ex);
        }
    }

    @PutMapping("/api/admin/class-applications/{id}/assign")
    public ResponseEntity<?> assign(@PathVariable Long id,
                                    @RequestBody Map<String, Object> payload,
                                    @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Object coachIdRaw = payload.get("coachId");
            if (coachIdRaw == null) return ResponseEntity.badRequest().body(Map.of("error", "coachId is required"));
            Long coachId = Long.valueOf(coachIdRaw.toString());
            return service.assign(id, coachId, email(authHeader))
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (AccessDeniedException ex) {
            return unauthorizedOrForbidden(ex);
        }
    }

    @PutMapping("/api/admin/class-applications/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Long id,
                                    @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            return service.reject(id, email(authHeader))
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (AccessDeniedException ex) {
            return unauthorizedOrForbidden(ex);
        }
    }

    private Map<String, Object> coachCard(User coach) {
        return Map.of(
                "id", coach.getId(),
                "name", value(coach.getName(), "Coach"),
                "title", value(coach.getChessTitle(), "EduChess Coach"),
                "rating", coach.getRating() == null ? "Development Coach" : coach.getRating(),
                "achievements", value(coach.getGoals(), "Tournament preparation, tactics training, and student progress coaching."),
                "background", value(coach.getBio(), "Experienced chess educator focused on structured improvement."),
                "teachingStyle", value(coach.getPlayingStyle(), "Patient, practical, and game-review focused."),
                "classType", "Online / school / tournament training",
                "profilePicture", coach.getProfilePicture() == null ? "" : coach.getProfilePicture()
        );
    }

    private Object value(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String email(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        try {
            return jwtUtil.extractEmail(authHeader.substring(7));
        } catch (Exception ex) {
            return null;
        }
    }

    private ResponseEntity<Map<String, String>> unauthorizedOrForbidden(AccessDeniedException ex) {
        if ("Unauthorized".equalsIgnoreCase(ex.getMessage())) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
    }
}
