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
        if (!"PAID".equalsIgnoreCase(payment.getStatus()))
            return ResponseEntity.status(409).body(err("A receipt is available only after the payment is completed"));

        payment = paymentService.ensureReceiptNumber(payment);
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
        if (!"PAID".equalsIgnoreCase(payment.getStatus()))
            return ResponseEntity.status(409).body(err("A receipt is available only after the payment is completed"));

        payment = paymentService.ensureReceiptNumber(payment);

        try {
            byte[] pdf = paymentReportService.generateReceiptPdf(payment);
            String filename = (payment.getReceiptNumber() != null ? payment.getReceiptNumber() : "educhess-payment-" + id) + ".pdf";
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .body(pdf);
        } catch (IOException ex) {
            return ResponseEntity.status(500).body(err("Could not generate PDF receipt"));
        }
    }

    /**
     * Admin-only: record a payment that was settled outside the gateway (cash, bank transfer).
     * Students pay through Stripe Checkout via {@code POST /{id}/checkout}.
     */
    @PostMapping("/{id}/pay")
    public ResponseEntity<?> recordOfflinePayment(@RequestHeader("Authorization") String auth,
                                                  @PathVariable Long id,
                                                  @RequestParam(value = "method", required = false) String method) {
        User user = resolve(auth);
        if (user == null) return ResponseEntity.status(401).body(err("Unauthorized"));
        if (user.getRole() != Role.ADMIN)
            return ResponseEntity.status(403).body(err("Admin access required"));

        Payment payment = paymentRepo.findById(id).orElse(null);
        if (payment == null) return ResponseEntity.notFound().build();
        if ("PAID".equals(payment.getStatus()))
            return ResponseEntity.badRequest().body(err("Already paid"));

        return ResponseEntity.ok(paymentService.markPaidManually(payment, method));
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
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(503).body(err(ex.getMessage()));
        } catch (StripeException ex) {
            return ResponseEntity.status(502).body(err("Stripe checkout failed: " + ex.getMessage()));
        }
    }

    /** Stripe server-to-server webhook. Public route; the signature header is the auth. */
    @PostMapping("/webhook")
    public ResponseEntity<String> stripeWebhook(@RequestBody String payload,
                                                @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        if (!paymentService.webhookConfigured()) {
            return ResponseEntity.status(503).body("webhook secret not configured");
        }
        if (signature == null || signature.isBlank()) {
            return ResponseEntity.badRequest().body("missing Stripe-Signature header");
        }
        String sessionId;
        try {
            sessionId = paymentService.extractPaidCheckoutSessionId(payload, signature).orElse(null);
        } catch (com.stripe.exception.SignatureVerificationException ex) {
            return ResponseEntity.badRequest().body("invalid signature");
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("could not parse event");
        }
        if (sessionId != null) {
            try {
                paymentService.markPaidFromCheckoutSession(sessionId);
            } catch (Exception ex) {
                // 200 anyway: a retry will not help a data problem, and the success
                // redirect is a second confirmation path.
                return ResponseEntity.ok("received; confirmation deferred");
            }
        }
        return ResponseEntity.ok("ok");
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
        map.put("receiptNumber", payment.getReceiptNumber());
        map.put("studentName", payment.getStudentName());
        map.put("month", payment.getMonth());
        map.put("sessionCount", payment.getSessionCount());
        map.put("totalAmount", payment.getTotalAmount());
        map.put("status", payment.getStatus());
        map.put("paidAt", payment.getPaidAt());
        map.put("receiptIssuedAt", payment.getReceiptIssuedAt());
        map.put("paymentMethod", payment.getPaymentMethod());
        map.put("stripeCheckoutSessionId", payment.getStripeCheckoutSessionId());
        map.put("stripePaymentIntentId", payment.getStripePaymentIntentId());
        return map;
    }

    private Map<String, String> err(String msg) { return Map.of("error", msg); }
}
