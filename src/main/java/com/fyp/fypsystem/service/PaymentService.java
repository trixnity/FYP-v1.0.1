package com.fyp.fypsystem.service;

import com.fyp.fypsystem.model.Payment;
import com.fyp.fypsystem.model.SessionPlan;
import com.fyp.fypsystem.repository.PaymentRepository;
import com.fyp.fypsystem.repository.SessionPlanRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.Map;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final SessionPlanRepository sessionPlanRepository;
    private final String currency;
    private final String successUrl;
    private final String cancelUrl;
    private final String webhookSecret;

    public PaymentService(PaymentRepository paymentRepository,
                          SessionPlanRepository sessionPlanRepository,
                          @Value("${stripe.currency:myr}") String currency,
                          @Value("${stripe.success-url:}") String successUrl,
                          @Value("${stripe.cancel-url:}") String cancelUrl,
                          @Value("${stripe.webhook-secret:}") String webhookSecret,
                          @Value("${app.base-url:}") String appBaseUrl) {
        this.paymentRepository = paymentRepository;
        this.sessionPlanRepository = sessionPlanRepository;
        this.currency = currency;
        this.webhookSecret = webhookSecret == null ? "" : webhookSecret.trim();
        String normalizedBaseUrl = appBaseUrl == null ? "" : appBaseUrl.replaceAll("/+$", "");
        if (successUrl == null || successUrl.isBlank()) {
            successUrl = normalizedBaseUrl.isBlank() ? null : normalizedBaseUrl + "/api/payments/checkout/success?session_id={CHECKOUT_SESSION_ID}";
        }
        if (cancelUrl == null || cancelUrl.isBlank()) {
            cancelUrl = normalizedBaseUrl.isBlank() ? null : normalizedBaseUrl + "/api/payments/checkout/cancel";
        }
        // Left nullable on purpose: payments are an optional integration. The app must
        // still boot (dashboard, puzzles, analysis) when Stripe/APP_BASE_URL are unset;
        // createCheckoutSession() reports the misconfiguration instead.
        this.successUrl = successUrl;
        this.cancelUrl = cancelUrl;
    }

    @Transactional
    public Session createCheckoutSession(Payment payment) throws StripeException {
        if (successUrl == null || cancelUrl == null) {
            throw new IllegalStateException("Online payments are not configured. Set APP_BASE_URL "
                    + "(or STRIPE_SUCCESS_URL / STRIPE_CANCEL_URL) and STRIPE_SECRET_KEY.");
        }
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

    /**
     * Confirms a payment from a Stripe Checkout Session id. Idempotent: a payment that is
     * already PAID is returned unchanged, so the browser redirect and the webhook can both
     * call this without double-activating the plan or re-issuing the receipt.
     */
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

        if ("PAID".equalsIgnoreCase(payment.getStatus())) {
            return payment;
        }

        payment.setStatus("PAID");
        payment.setPaidAt(LocalDateTime.now().toString());
        payment.setStripeCheckoutSessionId(session.getId());
        if (session.getPaymentIntent() != null) {
            payment.setStripePaymentIntentId(session.getPaymentIntent());
            payment.setPaymentMethod(describePaymentMethod(session.getPaymentIntent()));
        }
        assignReceiptNumber(payment);
        Payment saved = paymentRepository.save(payment);

        if (saved.getSessionPlanId() != null) {
            sessionPlanRepository.findById(saved.getSessionPlanId()).ifPresent(this::activatePlan);
        }

        return saved;
    }

    @Transactional
    public Payment markPaidManually(Payment payment, String method) {
        if ("PAID".equalsIgnoreCase(payment.getStatus())) {
            return payment;
        }
        payment.setStatus("PAID");
        payment.setPaidAt(LocalDateTime.now().toString());
        if (payment.getPaymentMethod() == null || payment.getPaymentMethod().isBlank()) {
            payment.setPaymentMethod(method == null || method.isBlank() ? "Manual / offline" : method);
        }
        assignReceiptNumber(payment);
        Payment saved = paymentRepository.save(payment);
        if (saved.getSessionPlanId() != null) {
            sessionPlanRepository.findById(saved.getSessionPlanId()).ifPresent(this::activatePlan);
        }
        return saved;
    }

    public boolean webhookConfigured() {
        return !webhookSecret.isBlank();
    }

    /**
     * Verifies a Stripe webhook signature and, for a completed checkout session, returns the
     * session id to confirm. Does no database work, so the caller can invoke the
     * {@code @Transactional} confirmation method through the Spring proxy.
     *
     * @throws com.stripe.exception.SignatureVerificationException if the signature is invalid
     */
    public java.util.Optional<String> extractPaidCheckoutSessionId(String payload, String signatureHeader)
            throws com.stripe.exception.SignatureVerificationException {
        Event event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        switch (event.getType()) {
            case "checkout.session.completed":
            case "checkout.session.async_payment_succeeded":
                java.util.Optional<String> sessionId = event.getDataObjectDeserializer().getObject()
                        .filter(o -> o instanceof Session)
                        .map(o -> ((Session) o).getId());
                if (sessionId.isEmpty()) {
                    log.warn("Stripe webhook {} had no session id", event.getType());
                }
                return sessionId;
            default:
                return java.util.Optional.empty();
        }
    }

    /** Assigns a receipt number to a legacy PAID payment that predates the receipt-number field. */
    @Transactional
    public Payment ensureReceiptNumber(Payment payment) {
        if ("PAID".equalsIgnoreCase(payment.getStatus())
                && (payment.getReceiptNumber() == null || payment.getReceiptNumber().isBlank())) {
            assignReceiptNumber(payment);
            return paymentRepository.save(payment);
        }
        return payment;
    }

    private void assignReceiptNumber(Payment payment) {
        if (payment.getReceiptNumber() != null && !payment.getReceiptNumber().isBlank()) {
            return;
        }
        payment.setReceiptNumber(String.format("EDU-%d-%06d", receiptYear(payment), payment.getId()));
        payment.setReceiptIssuedAt(LocalDateTime.now().toString());
    }

    /** Year the payment was settled (for backfilled legacy rows), falling back to now. */
    private int receiptYear(Payment payment) {
        String paidAt = payment.getPaidAt();
        if (paidAt != null && paidAt.length() >= 4) {
            try {
                return Integer.parseInt(paidAt.substring(0, 4));
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return Year.now().getValue();
    }

    private String describePaymentMethod(String paymentIntentId) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            String chargeId = intent.getLatestCharge();
            if (chargeId == null) {
                return null;
            }
            Charge charge = Charge.retrieve(chargeId);
            Charge.PaymentMethodDetails details = charge.getPaymentMethodDetails();
            if (details == null) {
                return null;
            }
            if (details.getCard() != null) {
                String brand = details.getCard().getBrand();
                String last4 = details.getCard().getLast4();
                return (brand == null ? "Card" : brand.toUpperCase()) + (last4 == null ? "" : " ****" + last4);
            }
            if (details.getFpx() != null) {
                String bank = details.getFpx().getBank();
                return bank == null ? "FPX" : "FPX (" + bank + ")";
            }
            return details.getType() == null ? null : details.getType().toUpperCase();
        } catch (Exception ex) {
            log.warn("Could not resolve payment method for {}: {}", paymentIntentId, ex.getMessage());
            return null;
        }
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
