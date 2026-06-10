package com.fyp.fypsystem.config;

import com.fyp.fypsystem.model.Role;
import com.fyp.fypsystem.model.User;
import com.fyp.fypsystem.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail("admin@educhess.com").isEmpty()) {
            User admin = new User(
                    "Admin",
                    "admin@educhess.com",
                    passwordEncoder.encode("admin123"),
                    null,
                    null,
                    Role.ADMIN
            );
            userRepository.save(admin);
            System.out.println("[SCETS] Default admin account created: admin@educhess.com");
        }
    }
}
