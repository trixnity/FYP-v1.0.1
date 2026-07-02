package com.fyp.fypsystem.service;

import com.fyp.fypsystem.model.Payment;
import com.fyp.fypsystem.model.SessionPlan;
import com.fyp.fypsystem.repository.PaymentRepository;
import com.fyp.fypsystem.repository.SessionPlanRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SessionPlanRepository sessionPlanRepository;
    private final String currency;
    private final String successUrl;
    private final String cancelUrl;

    public PaymentService(PaymentRepository paymentRepository,
                          SessionPlanRepository sessionPlanRepository,
                          @Value("${stripe.currency:myr}") String currency,
                          @Value("${stripe.success-url:}") String successUrl,
                          @Value("${stripe.cancel-url:}") String cancelUrl,
                          @Value("${app.base-url:}") String appBaseUrl) {
        this.paymentRepository = paymentRepository;
        this.sessionPlanRepository = sessionPlanRepository;
        this.currency = currency;
        String normalizedBaseUrl = appBaseUrl == null ? "" : appBaseUrl.replaceAll("/+$", "");
        if (successUrl == null || successUrl.isBlank()) {
            successUrl = normalizedBaseUrl.isBlank() ? null : normalizedBaseUrl + "/api/payments/checkout/success?session_id={CHECKOUT_SESSION_ID}";
        }
        if (cancelUrl == null || cancelUrl.isBlank()) {
            cancelUrl = normalizedBaseUrl.isBlank() ? null : normalizedBaseUrl + "/api/payments/checkout/cancel";
        }
        this.successUrl = Objects.requireNonNull(successUrl, "stripe.success-url or APP_BASE_URL must be configured");
        this.cancelUrl = Objects.requireNonNull(cancelUrl, "stripe.cancel-url or APP_BASE_URL must be configured");
    }

    @Transactional
    public Session createCheckoutSession(Payment payment) throws StripeException {
        long amountInSmallestUnit = BigDecimal.valueOf(payment.getTotalAmount() != null ? payment.getTotalAmount() : 0.0)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

        if (amountInSmallestUnit <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }

        String productName = "EduChess Sessions";
        if (payment.getMonth() != null && !payment.getMonth().isBlank()) {
            productName += " - " + payment.getMonth();
        }

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl + "?payment_id=" + payment.getId())
                .putMetadata("paymentId", String.valueOf(payment.getId()))
                .putMetadata("studentId", String.valueOf(payment.getStudentId()))
                .putMetadata("sessionPlanId", payment.getSessionPlanId() != null ? String.valueOf(payment.getSessionPlanId()) : "")
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency(currency)
                                                .setUnitAmount(amountInSmallestUnit)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(productName)
                                                                .setDescription((payment.getSessionCount() != null ? payment.getSessionCount() : 0) + " coaching sessions")
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .build();

        Session session = Session.create(params);
        payment.setStripeCheckoutSessionId(session.getId());
        paymentRepository.save(payment);
        return session;
    }

    @Transactional
    public Payment markPaidFromCheckoutSession(String sessionId) throws StripeException {
        Session session = Session.retrieve(sessionId);
        if (!"paid".equalsIgnoreCase(session.getPaymentStatus())) {
            throw new IllegalStateException("Stripe session is not paid");
        }

        Payment payment = paymentRepository.findByStripeCheckoutSessionId(sessionId)
                .orElseGet(() -> findPaymentByMetadata(session.getMetadata()));

        if (payment == null) {
            throw new IllegalArgumentException("Payment not found for Stripe session");
        }

        payment.setStatus("PAID");
        payment.setPaidAt(LocalDateTime.now().toString());
        payment.setStripeCheckoutSessionId(session.getId());
        if (session.getPaymentIntent() != null) {
            payment.setStripePaymentIntentId(session.getPaymentIntent());
        }
        Payment saved = paymentRepository.save(payment);

        if (saved.getSessionPlanId() != null) {
            sessionPlanRepository.findById(saved.getSessionPlanId()).ifPresent(this::activatePlan);
        }

        return saved;
    }

    @Transactional
    public Payment markPaidManually(Payment payment) {
        payment.setStatus("PAID");
        payment.setPaidAt(LocalDateTime.now().toString());
        Payment saved = paymentRepository.save(payment);
        if (saved.getSessionPlanId() != null) {
            sessionPlanRepository.findById(saved.getSessionPlanId()).ifPresent(this::activatePlan);
        }
        return saved;
    }

    private Payment findPaymentByMetadata(Map<String, String> metadata) {
        if (metadata == null) {
            return null;
        }
        String paymentId = metadata.get("paymentId");
        if (paymentId == null || paymentId.isBlank()) {
            return null;
        }
        try {
            return paymentRepository.findById(Long.parseLong(paymentId)).orElse(null);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void activatePlan(SessionPlan plan) {
        plan.setStatus("ACTIVE");
        sessionPlanRepository.save(plan);
    }
}
