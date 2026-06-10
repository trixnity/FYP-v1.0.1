package com.fyp.fypsystem.controller;

import com.fyp.fypsystem.model.Role;
import com.fyp.fypsystem.model.User;
import com.fyp.fypsystem.model.ZoomSession;
import com.fyp.fypsystem.repository.UserRepository;
import com.fyp.fypsystem.repository.ZoomSessionRepository;
import com.fyp.fypsystem.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/sessions")
@CrossOrigin(origins = "*")
public class SessionController {

    private final ZoomSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public SessionController(ZoomSessionRepository sessionRepository,
                             UserRepository userRepository,
                             JwtUtil jwtUtil) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    // Coach: create a session
    @PostMapping
    public ResponseEntity<?> create(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> body) {

        User coach = resolveUser(authHeader);
        if (coach == null || coach.getRole() != Role.COACH) {
            return ResponseEntity.status(403).body(Map.of("error", "Coach access required"));
        }

        String title = (String) body.get("title");
        String zoomLink = (String) body.get("zoomLink");
        String scheduledAtStr = (String) body.get("scheduledAt");

        if (title == null || title.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "title is required"));
        }
        if (zoomLink == null || zoomLink.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "zoomLink is required"));
        }
        if (scheduledAtStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "scheduledAt is required"));
        }

        LocalDateTime scheduledAt;
        try {
            scheduledAt = LocalDateTime.parse(scheduledAtStr);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid scheduledAt format. Use ISO-8601 (e.g. 2026-06-15T14:00:00)"));
        }

        ZoomSession session = new ZoomSession();
        session.setCoachId(coach.getId());
        session.setCoachName(coach.getName());
        session.setTitle(title);
        session.setDescription((String) body.get("description"));
        session.setZoomLink(zoomLink);
        session.setScheduledAt(scheduledAt);

        return ResponseEntity.ok(sessionRepository.save(session));
    }

    // Coach: see all their own sessions
    @GetMapping("/coach")
    public ResponseEntity<?> coachSessions(
            @RequestHeader("Authorization") String authHeader) {

        User coach = resolveUser(authHeader);
        if (coach == null || coach.getRole() != Role.COACH) {
            return ResponseEntity.status(403).body(Map.of("error", "Coach access required"));
        }
        return ResponseEntity.ok(sessionRepository.findByCoachId(coach.getId()));
    }

    // Student: see upcoming sessions from their assigned coach
    @GetMapping("/upcoming")
    public ResponseEntity<?> upcomingSessions(
            @RequestHeader("Authorization") String authHeader) {

        User student = resolveUser(authHeader);
        if (student == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

        LocalDateTime now = LocalDateTime.now();

        if (student.getCoachId() != null) {
            // Student has a coach — show only that coach's sessions
            return ResponseEntity.ok(
                    sessionRepository.findByCoachIdAndScheduledAtAfter(student.getCoachId(), now));
        } else {
            // No coach assigned — show all upcoming sessions so new students can discover coaches
            return ResponseEntity.ok(
                    sessionRepository.findByScheduledAtAfterOrderByScheduledAtAsc(now));
        }
    }

    // Coach or Admin: delete a session
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {

        User caller = resolveUser(authHeader);
        if (caller == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

        Optional<ZoomSession> opt = sessionRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        ZoomSession s = opt.get();
        boolean isOwner = caller.getRole() == Role.COACH && s.getCoachId().equals(caller.getId());
        boolean isAdmin = caller.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            return ResponseEntity.status(403).body(Map.of("error", "Not authorized"));
        }

        sessionRepository.delete(s);
        return ResponseEntity.ok(Map.of("deleted", id));
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
}
