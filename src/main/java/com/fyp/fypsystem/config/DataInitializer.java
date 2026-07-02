package com.fyp.fypsystem.config;

import com.fyp.fypsystem.model.Role;
import com.fyp.fypsystem.model.User;
import com.fyp.fypsystem.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final String DEMO_COACH_PASSWORD = "Coach@123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CoachProfileCatalog coachProfileCatalog;

    public DataInitializer(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           CoachProfileCatalog coachProfileCatalog) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.coachProfileCatalog = coachProfileCatalog;
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

        seedDemoCoaches();
    }

    private void seedDemoCoaches() {
        for (CoachProfileCatalog.CoachProfile profile : coachProfileCatalog.profiles()) {
            User coach = userRepository.findByEmail(profile.email()).orElseGet(() -> {
                User created = new User();
                created.setEmail(profile.email());
                // Demo only: login with profile.username() + "@educhess.demo"; password is DEMO_COACH_PASSWORD.
                created.setPassword(passwordEncoder.encode(DEMO_COACH_PASSWORD));
                return created;
            });

            coach.setName(profile.name());
            coach.setRole(Role.COACH);
            coach.setChessUsername(profile.username());
            coach.setChessTitle(profile.title());
            coach.setRating(profile.rating());
            coach.setGoals(profile.achievement());
            coach.setBio(profile.background());
            coach.setPlayingStyle(profile.teachingStyle());
            coach.setProfilePicture(profile.profilePicture());
            userRepository.save(coach);
        }
    }
}
