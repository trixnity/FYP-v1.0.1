package com.fyp.fypsystem.controller;

import com.fyp.fypsystem.model.Role;
import com.fyp.fypsystem.model.User;
import com.fyp.fypsystem.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public User register(@RequestBody User user) {
        if (user.getRole() == null) {
            user.setRole(Role.STUDENT);
        }
        return userRepository.save(user);
    }

    @GetMapping
    public List<User> getAll() {
        return userRepository.findAll();
    }
}
