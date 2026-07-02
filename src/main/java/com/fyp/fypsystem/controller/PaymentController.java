package com.fyp.fypsystem.controller;

import com.fyp.fypsystem.model.*;
import com.fyp.fypsystem.repository.*;
import com.fyp.fypsystem.security.JwtUtil;
import com.fyp.fypsystem.service.PaymentReportService;
import com.fyp.fypsystem.service.PaymentService;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentRepository paymentRepo;
    private final UserRepository userRepo;
    private final JwtUtil jwtUtil;
    private final PaymentService paymentService;
    private final PaymentReportService paymentReportService;

    public PaymentController(PaymentRepository paymentRepo,
                             UserRepository userRepo,
                             JwtUtil jwtUtil,
                             PaymentService paymentService,
                             PaymentReportService paymentReportService) {
        this.paymentRepo = paymentRepo;
        this.userRepo = userRepo;
        this.jwtUtil = jwtUtil;
        this.paymentService = paymentService;
        this.paymentReportService = paymentReportService;
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

    @GetMapping("/{id}/receipt")
    public ResponseEntity<?> getReceipt(@RequestHeader("Authorization") String auth,
                                        @PathVariable Long id) {
        User user = resolve(auth);
        if (user == null) return ResponseEntity.status(401).body(err("Unauthorized"));

        Payment payment = paymentRepo.findById(id).orElse(null);
        if (payment == null) return ResponseEntity.notFound().build();
        if (!canAccess(user, payment)) return ResponseEntity.status(403).body(err("Forbidden"));

        return ResponseEntity.ok(receiptMap(payment));
    }

    @GetMapping("/{id}/receipt/pdf")
    public ResponseEntity<?> downloadReceiptPdf(@RequestHeader("Authorization") String auth,
                                                @PathVariable Long id) {
        User user = resolve(auth);
        if (user == null) return ResponseEntity.status(401).body(err("Unauthorized"));

        Payment payment = paymentRepo.findById(id).orElse(null);
        if (payment == null) return ResponseEntity.notFound().build();
        if (!canAccess(user, payment)) return ResponseEntity.status(403).body(err("Forbidden"));

        try {
            byte[] pdf = paymentReportService.generateReceiptPdf(payment);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=educhess-payment-" + id + ".pdf")
                    .body(pdf);
        } catch (IOException ex) {
            return ResponseEntity.status(500).body(err("Could not generate PDF receipt"));
        }
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

        payment = paymentService.markPaidManually(payment);

        return ResponseEntity.ok(payment);
    }

    @PostMapping("/{id}/checkout")
    public ResponseEntity<?> createCheckout(@RequestHeader("Authorization") String auth,
                                            @PathVariable Long id) {
        User user = resolve(auth);
        if (user == null) return ResponseEntity.status(401).body(err("Unauthorized"));

        Payment payment = paymentRepo.findById(id).orElse(null);
        if (payment == null) return ResponseEntity.notFound().build();
        if (!payment.getStudentId().equals(user.getId()))
            return ResponseEntity.status(403).body(err("This payment is not yours"));
        if ("PAID".equals(payment.getStatus()))
            return ResponseEntity.badRequest().body(err("Already paid"));

        try {
            Session session = paymentService.createCheckoutSession(payment);
            return ResponseEntity.ok(Map.of(
                    "checkoutUrl", session.getUrl(),
                    "sessionId", session.getId()
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(err(ex.getMessage()));
        } catch (StripeException ex) {
            return ResponseEntity.status(502).body(err("Stripe checkout failed: " + ex.getMessage()));
        }
    }

    @GetMapping("/checkout/success")
    public ResponseEntity<?> checkoutSuccess(@RequestParam("session_id") String sessionId) {
        try {
            paymentService.markPaidFromCheckoutSession(sessionId);
            return ResponseEntity.status(303)
                    .location(URI.create("/dashboard.html?payment=success"))
                    .build();
        } catch (Exception ex) {
            return ResponseEntity.status(303)
                    .location(URI.create("/dashboard.html?payment=failed"))
                    .build();
        }
    }

    @GetMapping("/checkout/cancel")
    public ResponseEntity<?> checkoutCancel(@RequestParam(value = "payment_id", required = false) Long paymentId) {
        String suffix = paymentId != null ? "?payment=cancelled&paymentId=" + paymentId : "?payment=cancelled";
        return ResponseEntity.status(303)
                .location(URI.create("/dashboard.html" + suffix))
                .build();
    }

    private User resolve(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) return null;
        try { return userRepo.findByEmail(jwtUtil.extractEmail(auth.substring(7))).orElse(null); }
        catch (Exception e) { return null; }
    }

    private boolean canAccess(User user, Payment payment) {
        if (user.getRole() == Role.ADMIN) return true;
        if (user.getRole() == Role.COACH) return payment.getCoachId() != null && payment.getCoachId().equals(user.getId());
        return payment.getStudentId() != null && payment.getStudentId().equals(user.getId());
    }

    private Map<String, Object> receiptMap(Payment payment) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", payment.getId());
        map.put("studentName", payment.getStudentName());
        map.put("month", payment.getMonth());
        map.put("sessionCount", payment.getSessionCount());
        map.put("totalAmount", payment.getTotalAmount());
        map.put("status", payment.getStatus());
        map.put("paidAt", payment.getPaidAt());
        map.put("stripeCheckoutSessionId", payment.getStripeCheckoutSessionId());
        map.put("stripePaymentIntentId", payment.getStripePaymentIntentId());
        return map;
    }

    private Map<String, String> err(String msg) { return Map.of("error", msg); }
}
