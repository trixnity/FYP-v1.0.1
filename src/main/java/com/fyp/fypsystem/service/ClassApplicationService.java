package com.fyp.fypsystem.service;

import com.fyp.fypsystem.config.CoachProfileCatalog;
import com.fyp.fypsystem.model.ClassApplication;
import com.fyp.fypsystem.model.Role;
import com.fyp.fypsystem.model.User;
import com.fyp.fypsystem.repository.ClassApplicationRepository;
import com.fyp.fypsystem.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class ClassApplicationService {

    private final ClassApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final CoachProfileCatalog coachProfileCatalog;

    public ClassApplicationService(ClassApplicationRepository applicationRepository,
                                   UserRepository userRepository,
                                   CoachProfileCatalog coachProfileCatalog) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.coachProfileCatalog = coachProfileCatalog;
    }

    public List<User> coaches() {
        return coachProfileCatalog.profiles().stream()
                .map(profile -> userRepository.findByEmail(profile.email()))
                .flatMap(Optional::stream)
                .filter(user -> user.getRole() == Role.COACH)
                .toList();
    }

    public ClassApplication submit(ClassApplication payload, String email) {
        User student = requireRole(email, Role.STUDENT);
        ClassApplication app = new ClassApplication();
        app.setStudentId(student.getId());
        app.setStudentName(clean(payload.getStudentName()) != null ? clean(payload.getStudentName()) : student.getName());
        app.setEmail(clean(payload.getEmail()) != null ? clean(payload.getEmail()) : student.getEmail());
        app.setPhone(clean(payload.getPhone()) != null ? clean(payload.getPhone()) : student.getPhone());
        app.setAge(payload.getAge());
        app.setChessLevel(clean(payload.getChessLevel()));
        app.setPreferredCoachId(payload.getPreferredCoachId());
        app.setPreferredCoachName(resolveCoachName(payload.getPreferredCoachId(), payload.getPreferredCoachName()));
        app.setPreferredClassType(clean(payload.getPreferredClassType()));
        app.setPreferredSchedule(clean(payload.getPreferredSchedule()));
        app.setGoal(clean(payload.getGoal()));
        app.setNotes(payload.getNotes());
        app.setStatus(ClassApplication.ApplicationStatus.PENDING);
        return applicationRepository.save(app);
    }

    public List<ClassApplication> mine(String email) {
        User student = requireRole(email, Role.STUDENT);
        return applicationRepository.findByStudentIdOrderByCreatedAtDesc(student.getId());
    }

    public List<ClassApplication> allForAdmin(String email, ClassApplication.ApplicationStatus status) {
        requireRole(email, Role.ADMIN);
        if (status != null) return applicationRepository.findByStatusOrderByCreatedAtDesc(status);
        return applicationRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<ClassApplication> assignedToCoach(String email) {
        User coach = requireRole(email, Role.COACH);
        return applicationRepository.findByAssignedCoachIdOrderByUpdatedAtDesc(coach.getId());
    }

    public Optional<ClassApplication> assign(Long id, Long coachId, String email) {
        requireRole(email, Role.ADMIN);
        User coach = userRepository.findById(coachId)
                .filter(user -> user.getRole() == Role.COACH)
                .filter(user -> coachProfileCatalog.isOfficialCoachEmail(user.getEmail()))
                .orElseThrow(() -> new IllegalArgumentException("Coach not found"));
        return applicationRepository.findById(id).map(app -> {
            app.setAssignedCoachId(coach.getId());
            app.setAssignedCoachName(coach.getName());
            app.setStatus(ClassApplication.ApplicationStatus.ASSIGNED);
            if (app.getStudentId() != null) {
                userRepository.findById(app.getStudentId()).ifPresent(student -> {
                    if (student.getRole() == Role.STUDENT) {
                        student.setCoachId(coach.getId());
                        userRepository.save(student);
                    }
                });
            }
            return applicationRepository.save(app);
        });
    }

    public Optional<ClassApplication> reject(Long id, String email) {
        requireRole(email, Role.ADMIN);
        return applicationRepository.findById(id).map(app -> {
            app.setStatus(ClassApplication.ApplicationStatus.REJECTED);
            return applicationRepository.save(app);
        });
    }

    private User requireRole(String email, Role role) {
        if (email == null || email.isBlank()) throw new AccessDeniedException("Unauthorized");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Unauthorized"));
        if (user.getRole() != role) throw new AccessDeniedException(role + " access required");
        return user;
    }

    private String resolveCoachName(Long coachId, String fallback) {
        if (coachId == null) return clean(fallback);
        return userRepository.findById(coachId)
                .filter(user -> user.getRole() == Role.COACH)
                .filter(user -> coachProfileCatalog.isOfficialCoachEmail(user.getEmail()))
                .map(User::getName)
                .orElse(clean(fallback));
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
