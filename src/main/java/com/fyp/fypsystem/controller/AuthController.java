package com.fyp.fypsystem.controller;

import com.fyp.fypsystem.dto.LoginRequest;
import com.fyp.fypsystem.dto.LoginResponse;
import com.fyp.fypsystem.model.Role;
import com.fyp.fypsystem.model.User;
import com.fyp.fypsystem.repository.UserRepository;
import com.fyp.fypsystem.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        String identifier = request.getEmail() == null ? "" : request.getEmail().trim();
        Optional<User> userOpt = userRepository.findByEmail(identifier);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByChessUsername(identifier);
        }
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }
        User user = userOpt.get();
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return ResponseEntity.ok(new LoginResponse(
                token, user.getRole().name(), user.getEmail(), user.getName(), user.getId()
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.status(409).body(Map.of("error", "Email already exists"));
        }
        if (user.getRole() == null) {
            user.setRole(Role.STUDENT);
        }
        // Only STUDENT and COACH can self-register; ADMIN is created by the system
        if (user.getRole() == Role.ADMIN) {
            user.setRole(Role.STUDENT);
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User saved = userRepository.save(user);
        return ResponseEntity.ok(Map.of(
                "id", saved.getId(),
                "email", saved.getEmail(),
                "name", saved.getName(),
                "role", saved.getRole().name()
        ));
    }
}
