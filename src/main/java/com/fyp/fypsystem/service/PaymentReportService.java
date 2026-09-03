package com.fyp.fypsystem.service;

import com.fyp.fypsystem.model.Payment;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class PaymentReportService {

    private static final float LEFT = 50;
    private static final float RIGHT = 545;

    private final String issuerName;
    private final String issuerRegistrationNo;
    private final String issuerAddress;
    private final String issuerEmail;
    private final String issuerPhone;
    private final String taxNote;
    private final String currencyPrefix;

    public PaymentReportService(
            @Value("${receipt.issuer.name:EduChess Academy}") String issuerName,
            @Value("${receipt.issuer.registration-no:}") String issuerRegistrationNo,
            @Value("${receipt.issuer.address:}") String issuerAddress,
            @Value("${receipt.issuer.email:}") String issuerEmail,
            @Value("${receipt.issuer.phone:}") String issuerPhone,
            @Value("${receipt.tax-note:}") String taxNote,
            @Value("${receipt.currency-prefix:RM}") String currencyPrefix) {
        this.issuerName = blankToDash(issuerName, "EduChess Academy");
        this.issuerRegistrationNo = trimToEmpty(issuerRegistrationNo);
        this.issuerAddress = trimToEmpty(issuerAddress);
        this.issuerEmail = trimToEmpty(issuerEmail);
        this.issuerPhone = trimToEmpty(issuerPhone);
        this.taxNote = trimToEmpty(taxNote);
        this.currencyPrefix = blankToDash(currencyPrefix, "RM");
    }

    public byte[] generateReceiptPdf(Payment payment) throws IOException {
        double amount = payment.getTotalAmount() != null ? payment.getTotalAmount() : 0.0;

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font body = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream c = new PDPageContentStream(document, page)) {
                float y = 800;

                // Issuer header
                text(c, bold, 16, LEFT, y, issuerName);
                y -= 16;
                if (!issuerRegistrationNo.isEmpty()) { text(c, body, 9, LEFT, y, "Registration No: " + issuerRegistrationNo); y -= 12; }
                for (String line : issuerAddress.split("\\r?\\n")) {
                    if (!line.isBlank()) { text(c, body, 9, LEFT, y, line.trim()); y -= 12; }
                }
                String contact = joinNonEmpty("  |  ", issuerEmail, issuerPhone);
                if (!contact.isEmpty()) { text(c, body, 9, LEFT, y, contact); y -= 12; }

                y -= 14;
                line(c, y);
                y -= 26;

                // Title + receipt meta
                text(c, bold, 15, LEFT, y, "OFFICIAL RECEIPT");
                y -= 22;
                y = kv(c, bold, body, "Receipt No", dash(payment.getReceiptNumber()), y);
                y = kv(c, bold, body, "Issue Date", formatDateTime(firstNonBlank(payment.getReceiptIssuedAt(), payment.getPaidAt())), y);
                y = kv(c, bold, body, "Payment Date", formatDateTime(payment.getPaidAt()), y);
                y = kv(c, bold, body, "Received From", dash(payment.getStudentName()), y);

                y -= 8;
                line(c, y);
                y -= 22;

                // Line item table
                text(c, bold, 10, LEFT, y, "Description");
                textRight(c, bold, 10, RIGHT, y, currencyPrefix + " " + money(amount));
                y -= 16;
                String desc = "Chess coaching sessions"
                        + (isBlank(payment.getMonth()) ? "" : " - " + payment.getMonth())
                        + (payment.getSessionCount() != null ? " (" + payment.getSessionCount() + " sessions)" : "");
                text(c, body, 10, LEFT, y, desc);
                y -= 18;
                line(c, y);
                y -= 20;

                // Totals
                y = totalRow(c, body, "Subtotal", currencyPrefix + " " + money(amount), y);
                y = totalRow(c, body, "Tax", currencyPrefix + " " + money(0.0), y);
                y = totalRow(c, bold, "Total Paid", currencyPrefix + " " + money(amount), y);

                y -= 10;
                y = kv(c, bold, body, "Payment Method", dash(payment.getPaymentMethod()), y);
                y = kv(c, bold, body, "Payment Status", dash(payment.getStatus()), y);
                if (!isBlank(payment.getStripePaymentIntentId())) {
                    y = kv(c, bold, body, "Gateway Reference", payment.getStripePaymentIntentId(), y);
                }

                // Tax note (issuer-supplied; not a deductibility guarantee).
                // Stop before the footer band so a long note cannot overwrite it.
                float footerTop = 92;
                if (!taxNote.isEmpty() && y > footerTop + 28) {
                    y -= 16;
                    line(c, y);
                    y -= 16;
                    for (String wrapped : wrap(taxNote, 95)) {
                        if (y < footerTop) {
                            text(c, body, 9, LEFT, y, "...");
                            break;
                        }
                        text(c, body, 9, LEFT, y, wrapped);
                        y -= 12;
                    }
                }

                // Footer (fixed band at the bottom of the page)
                text(c, body, 8, LEFT, 60, "This is a computer-generated receipt and does not require a signature.");
                if (!isBlank(payment.getReceiptNumber())) {
                    text(c, body, 8, LEFT, 48, "Verify this receipt with " + issuerName + " quoting the receipt number above.");
                }
            }

            document.save(out);
            return out.toByteArray();
        }
    }

    // ---- drawing helpers ----

    private float kv(PDPageContentStream c, PDType1Font labelFont, PDType1Font valueFont,
                     String label, String value, float y) throws IOException {
        text(c, labelFont, 10, LEFT, y, label);
        text(c, valueFont, 10, LEFT + 130, y, safe(value));
        return y - 16;
    }

    private float totalRow(PDPageContentStream c, PDType1Font font, String label, String value, float y) throws IOException {
        text(c, font, 10, RIGHT - 240, y, label);
        textRight(c, font, 10, RIGHT, y, value);
        return y - 16;
    }

    private void text(PDPageContentStream c, PDType1Font font, float size, float x, float y, String s) throws IOException {
        c.beginText();
        c.setFont(font, size);
        c.newLineAtOffset(x, y);
        c.showText(safe(s));
        c.endText();
    }

    private void textRight(PDPageContentStream c, PDType1Font font, float size, float xRight, float y, String s) throws IOException {
        String safe = safe(s);
        float width = font.getStringWidth(safe) / 1000 * size;
        text(c, font, size, xRight - width, y, safe);
    }

    private void line(PDPageContentStream c, float y) throws IOException {
        c.moveTo(LEFT, y);
        c.lineTo(RIGHT, y);
        c.stroke();
    }

    // ---- string helpers ----

    private static String money(double v) {
        return String.format("%,.2f", v);
    }

    private static String formatDateTime(String iso) {
        if (isBlank(iso)) return "-";
        try {
            LocalDateTime dt = LocalDateTime.parse(iso.trim());
            return dt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));
        } catch (Exception ex) {
            return iso.length() > 19 ? iso.substring(0, 19).replace('T', ' ') : iso;
        }
    }

    private static String safe(String s) {
        String v = (s == null || s.isBlank()) ? "-" : s;
        return v.replace("\r", " ").replace("\n", " ").replace("\t", " ");
    }

    private static String dash(String s) { return isBlank(s) ? "-" : s.trim(); }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    private static String trimToEmpty(String s) { return s == null ? "" : s.trim(); }

    private static String blankToDash(String s, String fallback) { return isBlank(s) ? fallback : s.trim(); }

    private static String firstNonBlank(String a, String b) { return isBlank(a) ? b : a; }

    private static String joinNonEmpty(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p != null && !p.isBlank()) {
                if (sb.length() > 0) sb.append(sep);
                sb.append(p.trim());
            }
        }
        return sb.toString();
    }

    private static java.util.List<String> wrap(String text, int max) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (current.length() + word.length() + 1 > max) {
                lines.add(current.toString());
                current.setLength(0);
            }
            if (current.length() > 0) current.append(' ');
            current.append(word);
        }
        if (current.length() > 0) lines.add(current.toString());
        return lines;
    }
}
