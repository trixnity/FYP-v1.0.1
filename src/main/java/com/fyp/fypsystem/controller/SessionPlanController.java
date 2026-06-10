package com.fyp.fypsystem.controller;

import com.fyp.fypsystem.model.*;
import com.fyp.fypsystem.repository.*;
import com.fyp.fypsystem.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.*;

@RestController
@RequestMapping("/api/session-plans")
@CrossOrigin(origins = "*")
public class SessionPlanController {

    private final SessionPlanRepository planRepo;
    private final PaymentRepository paymentRepo;
    private final UserRepository userRepo;
    private final JwtUtil jwtUtil;

    public SessionPlanController(SessionPlanRepository planRepo,
                                 PaymentRepository paymentRepo,
                                 UserRepository userRepo,
                                 JwtUtil jwtUtil) {
        this.planRepo = planRepo;
        this.paymentRepo = paymentRepo;
        this.userRepo = userRepo;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/coach")
    public ResponseEntity<?> getCoachPlans(@RequestHeader("Authorization") String auth) {
        User coach = resolve(auth);
        if (coach == null || coach.getRole() != Role.COACH)
            return ResponseEntity.status(403).body(err("Coach access required"));

        List<SessionPlan> plans = planRepo.findByCoachIdOrderByCreatedAtDesc(coach.getId());
        List<Map<String, Object>> result = new ArrayList<>();
        for (SessionPlan p : plans) {
            Map<String, Object> m = planToMap(p);
            paymentRepo.findBySessionPlanId(p.getId()).ifPresent(pay -> m.put("payment", payToMap(pay)));
            result.add(m);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/student")
    public ResponseEntity<?> getStudentPlans(@RequestHeader("Authorization") String auth) {
        User student = resolve(auth);
        if (student == null) return ResponseEntity.status(401).body(err("Unauthorized"));

        List<SessionPlan> plans = planRepo.findByStudentIdOrderByCreatedAtDesc(student.getId());
        List<Map<String, Object>> result = new ArrayList<>();
        for (SessionPlan p : plans) {
            Map<String, Object> m = planToMap(p);
            paymentRepo.findBySessionPlanId(p.getId()).ifPresent(pay -> m.put("payment", payToMap(pay)));
            result.add(m);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestHeader("Authorization") String auth,
                                    @RequestBody Map<String, Object> body) {
        User coach = resolve(auth);
        if (coach == null || coach.getRole() != Role.COACH)
            return ResponseEntity.status(403).body(err("Coach access required"));

        Long studentId = toLong(body.get("studentId"));
        if (studentId == null) return ResponseEntity.badRequest().body(err("studentId required"));

        User student = userRepo.findById(studentId).orElse(null);
        if (student == null) return ResponseEntity.badRequest().body(err("Student not found"));

        String month = body.get("month") != null ? body.get("month").toString() : YearMonth.now().toString();

        if (planRepo.findByCoachIdAndStudentIdAndMonth(coach.getId(), studentId, month).isPresent())
            return ResponseEntity.badRequest().body(err("A session plan already exists for this student in " + month));

        Integer sessions = toInt(body.get("sessionsPerMonth"));
        if (sessions == null || sessions < 1) sessions = 4;

        Double price = toDouble(body.get("pricePerSession"));
        if (price == null || price < 0) price = 50.0;

        SessionPlan plan = new SessionPlan();
        plan.setCoachId(coach.getId());
        plan.setCoachName(coach.getName());
        plan.setStudentId(studentId);
        plan.setStudentName(student.getName());
        plan.setSessionsPerMonth(sessions);
        plan.setPricePerSession(price);
        plan.setMonth(month);
        plan.setNotes(body.get("notes") != null ? body.get("notes").toString() : null);
        SessionPlan saved = planRepo.save(plan);

        Payment payment = new Payment();
        payment.setStudentId(studentId);
        payment.setCoachId(coach.getId());
        payment.setSessionPlanId(saved.getId());
        payment.setStudentName(student.getName());
        payment.setMonth(month);
        payment.setSessionCount(sessions);
        payment.setTotalAmount(sessions * price);
        Payment savedPay = paymentRepo.save(payment);

        Map<String, Object> result = planToMap(saved);
        result.put("payment", payToMap(savedPay));
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@RequestHeader("Authorization") String auth,
                                    @PathVariable Long id) {
        User coach = resolve(auth);
        if (coach == null || coach.getRole() != Role.COACH)
            return ResponseEntity.status(403).body(err("Coach access required"));

        SessionPlan plan = planRepo.findById(id).orElse(null);
        if (plan == null) return ResponseEntity.notFound().build();
        if (!plan.getCoachId().equals(coach.getId()))
            return ResponseEntity.status(403).body(err("Forbidden"));

        paymentRepo.findBySessionPlanId(id).ifPresent(p -> {
            if ("PENDING".equals(p.getStatus())) paymentRepo.delete(p);
        });
        planRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Deleted"));
    }

    private Map<String, Object> planToMap(SessionPlan p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("coachId", p.getCoachId());
        m.put("coachName", p.getCoachName());
        m.put("studentId", p.getStudentId());
        m.put("studentName", p.getStudentName());
        m.put("sessionsPerMonth", p.getSessionsPerMonth());
        m.put("pricePerSession", p.getPricePerSession());
        m.put("totalAmount", p.getSessionsPerMonth() != null && p.getPricePerSession() != null
                ? p.getSessionsPerMonth() * p.getPricePerSession() : 0);
        m.put("month", p.getMonth());
        m.put("status", p.getStatus());
        m.put("notes", p.getNotes());
        m.put("createdAt", p.getCreatedAt());
        return m;
    }

    private Map<String, Object> payToMap(Payment p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("status", p.getStatus());
        m.put("totalAmount", p.getTotalAmount());
        m.put("sessionCount", p.getSessionCount());
        m.put("paidAt", p.getPaidAt());
        return m;
    }

    private User resolve(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) return null;
        try { return userRepo.findByEmail(jwtUtil.extractEmail(auth.substring(7))).orElse(null); }
        catch (Exception e) { return null; }
    }
    private Long toLong(Object v) { try { return v != null ? Long.parseLong(v.toString()) : null; } catch (Exception e) { return null; } }
    private Integer toInt(Object v) { try { return v != null ? Integer.parseInt(v.toString()) : null; } catch (Exception e) { return null; } }
    private Double toDouble(Object v) { try { return v != null ? Double.parseDouble(v.toString()) : null; } catch (Exception e) { return null; } }
    private Map<String, String> err(String msg) { return Map.of("error", msg); }
}
