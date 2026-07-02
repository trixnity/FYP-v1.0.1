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
    public ResponseEntity<?> register(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                      @RequestBody User user) {
        User requester = resolve(authHeader);
        if (requester == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        if (requester.getRole() != Role.ADMIN) return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
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
    public ResponseEntity<?> getAll(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        User requester = resolve(authHeader);
        if (requester == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        if (requester.getRole() != Role.ADMIN) return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        return ResponseEntity.ok(userRepository.findAll());
    }

    // Admin: edit any user except the currently logged-in admin
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id,
                                        @RequestHeader(value = "Authorization", required = false) String authHeader,
                                        @RequestBody Map<String, Object> body) {
        User requester = resolve(authHeader);
        if (requester == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        if (requester.getRole() != Role.ADMIN) return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        if (requester.getId().equals(id)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admins cannot edit their own account from User Management"));
        }

        return userRepository.findById(id).<ResponseEntity<?>>map(user -> {
            String email = clean(body.get("email"));
            if (email != null && !email.equalsIgnoreCase(user.getEmail()) && userRepository.findByEmail(email).isPresent()) {
                return ResponseEntity.status(409).body(Map.of("error", "Email already exists"));
            }
            if (body.containsKey("name")) user.setName(clean(body.get("name")));
            if (email != null) user.setEmail(email);
            if (body.containsKey("role")) {
                Role role = parseRole(body.get("role"));
                if (role != null) user.setRole(role);
            }
            if (body.containsKey("rating")) user.setRating(parseInteger(body.get("rating")));
            if (body.containsKey("goals")) user.setGoals(clean(body.get("goals")));
            if (body.containsKey("phone")) user.setPhone(clean(body.get("phone")));
            if (body.containsKey("chessUsername")) user.setChessUsername(clean(body.get("chessUsername")));
            if (body.containsKey("chessTitle")) user.setChessTitle(clean(body.get("chessTitle")));
            if (body.containsKey("password")) {
                String password = clean(body.get("password"));
                if (password != null) {
                    if (password.length() < 6) {
                        return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters"));
                    }
                    user.setPassword(passwordEncoder.encode(password));
                }
            }
            return ResponseEntity.ok(userRepository.save(user));
        }).orElse(ResponseEntity.status(404).body(Map.of("error", "User not found")));
    }

    // Admin: delete any user except the currently logged-in admin
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id,
                                        @RequestHeader(value = "Authorization", required = false) String authHeader) {
        User requester = resolve(authHeader);
        if (requester == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        if (requester.getRole() != Role.ADMIN) return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        if (requester.getId().equals(id)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admins cannot delete their own account"));
        }
        if (!userRepository.existsById(id)) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "User deleted"));
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

    // Admin: assign a coach to a student
    @PutMapping("/{studentId}/assign-coach/{coachId}")
    public ResponseEntity<?> assignCoach(@PathVariable Long studentId,
                                         @PathVariable Long coachId,
                                         @RequestHeader(value = "Authorization", required = false) String authHeader) {
        User requester = resolve(authHeader);
        if (requester == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        if (requester.getRole() != Role.ADMIN) return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));

        Optional<User> studentOpt = userRepository.findById(studentId);
        Optional<User> coachOpt   = userRepository.findById(coachId);

        if (studentOpt.isEmpty()) return ResponseEntity.notFound().build();
        if (studentOpt.get().getRole() != Role.STUDENT) {
            return ResponseEntity.badRequest().body(Map.of("error", "Target user is not a student"));
        }
        if (coachOpt.isEmpty() || coachOpt.get().getRole() != Role.COACH) {
            return ResponseEntity.badRequest().body(Map.of("error", "Target user is not a coach"));
        }

        User student = studentOpt.get();
        student.setCoachId(coachId);
        return ResponseEntity.ok(userRepository.save(student));
    }

    // Admin: remove coach assignment from a student
    @DeleteMapping("/{studentId}/assign-coach")
    public ResponseEntity<?> removeCoach(@PathVariable Long studentId,
                                         @RequestHeader(value = "Authorization", required = false) String authHeader) {
        User requester = resolve(authHeader);
        if (requester == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        if (requester.getRole() != Role.ADMIN) return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));

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

    private User resolve(String authHeader) {
        String email = extractEmail(authHeader);
        if (email == null) return null;
        return userRepository.findByEmail(email).orElse(null);
    }

    private String clean(Object value) {
        if (value == null) return null;
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private Integer parseInteger(Object value) {
        if (value == null || value.toString().isBlank()) return null;
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Role parseRole(Object value) {
        if (value == null) return null;
        try {
            return Role.valueOf(value.toString().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
