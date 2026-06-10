package com.fyp.fypsystem.controller;

import com.fyp.fypsystem.model.*;
import com.fyp.fypsystem.repository.*;
import com.fyp.fypsystem.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentRepository paymentRepo;
    private final SessionPlanRepository planRepo;
    private final UserRepository userRepo;
    private final JwtUtil jwtUtil;

    public PaymentController(PaymentRepository paymentRepo,
                             SessionPlanRepository planRepo,
                             UserRepository userRepo,
                             JwtUtil jwtUtil) {
        this.paymentRepo = paymentRepo;
        this.planRepo = planRepo;
        this.userRepo = userRepo;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/student")
    public ResponseEntity<?> getStudentPayments(@RequestHeader("Authorization") String auth) {
        User user = resolve(auth);
        if (user == null) return ResponseEntity.status(401).body(err("Unauthorized"));
        return ResponseEntity.ok(paymentRepo.findByStudentIdOrderByCreatedAtDesc(user.getId()));
    }

    @GetMapping("/coach")
    public ResponseEntity<?> getCoachPayments(@RequestHeader("Authorization") String auth) {
        User coach = resolve(auth);
        if (coach == null || coach.getRole() != Role.COACH)
            return ResponseEntity.status(403).body(err("Coach access required"));
        return ResponseEntity.ok(paymentRepo.findByCoachIdOrderByCreatedAtDesc(coach.getId()));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<?> pay(@RequestHeader("Authorization") String auth,
                                 @PathVariable Long id) {
        User user = resolve(auth);
        if (user == null) return ResponseEntity.status(401).body(err("Unauthorized"));

        Payment payment = paymentRepo.findById(id).orElse(null);
        if (payment == null) return ResponseEntity.notFound().build();
        if (!payment.getStudentId().equals(user.getId()))
            return ResponseEntity.status(403).body(err("This payment is not yours"));
        if ("PAID".equals(payment.getStatus()))
            return ResponseEntity.badRequest().body(err("Already paid"));

        payment.setStatus("PAID");
        payment.setPaidAt(java.time.LocalDateTime.now().toString());
        paymentRepo.save(payment);

        if (payment.getSessionPlanId() != null) {
            planRepo.findById(payment.getSessionPlanId()).ifPresent(plan -> {
                plan.setStatus("ACTIVE");
                planRepo.save(plan);
            });
        }

        return ResponseEntity.ok(payment);
    }

    private User resolve(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) return null;
        try { return userRepo.findByEmail(jwtUtil.extractEmail(auth.substring(7))).orElse(null); }
        catch (Exception e) { return null; }
    }
    private Map<String, String> err(String msg) { return Map.of("error", msg); }
}
