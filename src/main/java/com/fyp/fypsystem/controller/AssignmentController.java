package com.fyp.fypsystem.controller;

import com.fyp.fypsystem.model.Assignment;
import com.fyp.fypsystem.model.Puzzle;
import com.fyp.fypsystem.model.Role;
import com.fyp.fypsystem.model.User;
import com.fyp.fypsystem.repository.AssignmentRepository;
import com.fyp.fypsystem.repository.PuzzleRepository;
import com.fyp.fypsystem.repository.UserRepository;
import com.fyp.fypsystem.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/assignments")
@CrossOrigin(origins = "*")
public class AssignmentController {

    private final AssignmentRepository assignmentRepository;
    private final PuzzleRepository puzzleRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public AssignmentController(AssignmentRepository assignmentRepository,
                                PuzzleRepository puzzleRepository,
                                UserRepository userRepository,
                                JwtUtil jwtUtil) {
        this.assignmentRepository = assignmentRepository;
        this.puzzleRepository = puzzleRepository;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    // Coach: assign a puzzle to a student
    @PostMapping
    public ResponseEntity<?> assign(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> body) {

        User coach = resolveUser(authHeader);
        if (coach == null || coach.getRole() != Role.COACH) {
            return ResponseEntity.status(403).body(Map.of("error", "Coach access required"));
        }

        Long puzzleId  = toLong(body.get("puzzleId"));
        Long studentId = toLong(body.get("studentId"));
        if (puzzleId == null || studentId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "puzzleId and studentId are required"));
        }

        Optional<Puzzle> puzzleOpt  = puzzleRepository.findById(puzzleId);
        Optional<User>   studentOpt = userRepository.findById(studentId);

        if (puzzleOpt.isEmpty())  return ResponseEntity.badRequest().body(Map.of("error", "Puzzle not found"));
        if (studentOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Student not found"));
        if (studentOpt.get().getRole() != Role.STUDENT) {
            return ResponseEntity.badRequest().body(Map.of("error", "Target user is not a student"));
        }

        Assignment a = new Assignment();
        a.setCoachId(coach.getId());
        a.setStudentId(studentId);
        a.setPuzzleId(puzzleId);
        a.setPuzzleTitle(puzzleOpt.get().getTitle());
        a.setPuzzleTheme(puzzleOpt.get().getTheme());

        if (body.get("dueDate") != null) {
            try {
                a.setDueDate(LocalDateTime.parse(body.get("dueDate").toString()));
            } catch (Exception ignored) {}
        }

        return ResponseEntity.ok(assignmentRepository.save(a));
    }

    // Student: get their own assigned exercises
    @GetMapping("/my-assignments")
    public ResponseEntity<?> myAssignments(
            @RequestHeader("Authorization") String authHeader) {

        User student = resolveUser(authHeader);
        if (student == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

        List<Assignment> list = assignmentRepository.findByStudentId(student.getId());
        return ResponseEntity.ok(list);
    }

    // Coach: see all assignments they created
    @GetMapping("/coach")
    public ResponseEntity<?> coachAssignments(
            @RequestHeader("Authorization") String authHeader) {

        User coach = resolveUser(authHeader);
        if (coach == null || coach.getRole() != Role.COACH) {
            return ResponseEntity.status(403).body(Map.of("error", "Coach access required"));
        }

        return ResponseEntity.ok(assignmentRepository.findByCoachId(coach.getId()));
    }

    // Student: mark an assignment as completed
    @PutMapping("/{id}/complete")
    public ResponseEntity<?> complete(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {

        User student = resolveUser(authHeader);
        if (student == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

        Optional<Assignment> opt = assignmentRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Assignment a = opt.get();
        if (!a.getStudentId().equals(student.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "This assignment does not belong to you"));
        }

        a.setStatus(Assignment.AssignmentStatus.COMPLETED);
        a.setCompletedAt(LocalDateTime.now());
        return ResponseEntity.ok(assignmentRepository.save(a));
    }

    // Admin or Coach: delete an assignment
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {

        User caller = resolveUser(authHeader);
        if (caller == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

        Optional<Assignment> opt = assignmentRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Assignment a = opt.get();
        boolean isCoachOwner = caller.getRole() == Role.COACH && a.getCoachId().equals(caller.getId());
        boolean isAdmin = caller.getRole() == Role.ADMIN;

        if (!isCoachOwner && !isAdmin) {
            return ResponseEntity.status(403).body(Map.of("error", "Not authorized to delete this assignment"));
        }

        assignmentRepository.delete(a);
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

    private Long toLong(Object val) {
        if (val == null) return null;
        try { return Long.parseLong(val.toString()); }
        catch (NumberFormatException e) { return null; }
    }
}
