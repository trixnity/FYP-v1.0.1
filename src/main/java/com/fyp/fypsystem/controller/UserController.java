package com.fyp.fypsystem.controller;

import com.fyp.fypsystem.model.Role;
import com.fyp.fypsystem.model.User;
import com.fyp.fypsystem.repository.UserRepository;
import com.fyp.fypsystem.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    // Admin: create a user with any role (including ADMIN)
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.status(409).body(Map.of("error", "Email already exists"));
        }
        if (user.getRole() == null) {
            user.setRole(Role.STUDENT);
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return ResponseEntity.ok(userRepository.save(user));
    }

    // Admin: list all users (password is @JsonIgnore so it is never sent)
    @GetMapping
    public List<User> getAll() {
        return userRepository.findAll();
    }

    // Any authenticated user: get own profile
    @GetMapping("/me")
    public ResponseEntity<?> getMe(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String email = extractEmail(authHeader);
        if (email == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        return userRepository.findByEmail(email)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).body(Map.of("error", "User not found")));
    }

    // Coach: get all students assigned to the calling coach (reads email from JWT)
    @GetMapping("/my-students")
    public ResponseEntity<?> getMyStudents(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String email = extractEmail(authHeader);
        if (email == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

        Optional<User> coachOpt = userRepository.findByEmail(email);
        if (coachOpt.isEmpty() || coachOpt.get().getRole() != Role.COACH) {
            return ResponseEntity.status(403).body(Map.of("error", "Coach access required"));
        }

        List<User> students = userRepository.findByCoachIdAndRole(coachOpt.get().getId(), Role.STUDENT);
        return ResponseEntity.ok(students);
    }

    // Any user: update own extended profile fields
    @PutMapping("/me/profile")
    public ResponseEntity<?> updateProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> body) {
        String email = extractEmail(authHeader);
        if (email == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        return userRepository.findByEmail(email).map(user -> {
            if (body.containsKey("name") && body.get("name") != null && !body.get("name").toString().isBlank())
                user.setName(body.get("name").toString().trim());
            if (body.containsKey("bio"))
                user.setBio(body.get("bio") != null ? body.get("bio").toString() : null);
            if (body.containsKey("phone"))
                user.setPhone(body.get("phone") != null ? body.get("phone").toString() : null);
            if (body.containsKey("location"))
                user.setLocation(body.get("location") != null ? body.get("location").toString() : null);
            if (body.containsKey("chessUsername"))
                user.setChessUsername(body.get("chessUsername") != null ? body.get("chessUsername").toString() : null);
            if (body.containsKey("fideId"))
                user.setFideId(body.get("fideId") != null ? body.get("fideId").toString() : null);
            if (body.containsKey("playingStyle"))
                user.setPlayingStyle(body.get("playingStyle") != null ? body.get("playingStyle").toString() : null);
            if (body.containsKey("chessTitle"))
                user.setChessTitle(body.get("chessTitle") != null ? body.get("chessTitle").toString() : null);
            if (body.containsKey("favouriteWhiteOpening"))
                user.setFavouriteWhiteOpening(body.get("favouriteWhiteOpening") != null ? body.get("favouriteWhiteOpening").toString() : null);
            if (body.containsKey("favouriteBlackOpening"))
                user.setFavouriteBlackOpening(body.get("favouriteBlackOpening") != null ? body.get("favouriteBlackOpening").toString() : null);
            if (body.containsKey("goals"))
                user.setGoals(body.get("goals") != null ? body.get("goals").toString() : null);
            if (body.containsKey("profilePicture"))
                user.setProfilePicture(body.get("profilePicture") != null ? body.get("profilePicture").toString() : null);
            return ResponseEntity.<Object>ok(userRepository.save(user));
        }).orElse(ResponseEntity.status(404).body(Map.of("error", "User not found")));
    }

    // Any user: update own goals (kept for backwards compat)
    @PutMapping("/me/goals")
    public ResponseEntity<?> updateGoals(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> body) {
        String email = extractEmail(authHeader);
        if (email == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        return userRepository.findByEmail(email).map(user -> {
            user.setGoals((String) body.get("goals"));
            return ResponseEntity.<Object>ok(userRepository.save(user));
        }).orElse(ResponseEntity.status(404).body(Map.of("error", "User not found")));
    }

    // Any user: change own password
    @PutMapping("/me/password")
    public ResponseEntity<?> updatePassword(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> body) {
        String email = extractEmail(authHeader);
        if (email == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        String newPassword = (String) body.get("newPassword");
        if (newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters"));
        }
        return userRepository.findByEmail(email).map(user -> {
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return ResponseEntity.<Object>ok(Map.of("message", "Password updated successfully"));
        }).orElse(ResponseEntity.status(404).body(Map.of("error", "User not found")));
    }

    // Admin or Coach: assign a coach to a student
    @PutMapping("/{studentId}/assign-coach/{coachId}")
    public ResponseEntity<?> assignCoach(@PathVariable Long studentId,
                                         @PathVariable Long coachId) {
        Optional<User> studentOpt = userRepository.findById(studentId);
        Optional<User> coachOpt   = userRepository.findById(coachId);

        if (studentOpt.isEmpty()) return ResponseEntity.notFound().build();
        if (coachOpt.isEmpty() || coachOpt.get().getRole() != Role.COACH) {
            return ResponseEntity.badRequest().body(Map.of("error", "Target user is not a coach"));
        }

        User student = studentOpt.get();
        student.setCoachId(coachId);
        return ResponseEntity.ok(userRepository.save(student));
    }

    // Admin: remove coach assignment from a student
    @DeleteMapping("/{studentId}/assign-coach")
    public ResponseEntity<?> removeCoach(@PathVariable Long studentId) {
        Optional<User> studentOpt = userRepository.findById(studentId);
        if (studentOpt.isEmpty()) return ResponseEntity.notFound().build();

        User student = studentOpt.get();
        student.setCoachId(null);
        return ResponseEntity.ok(userRepository.save(student));
    }

    private String extractEmail(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        try {
            return jwtUtil.extractEmail(authHeader.substring(7));
        } catch (Exception e) {
            return null;
        }
    }
}
