package com.fyp.fypsystem.service;

import com.fyp.fypsystem.model.Payment;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class PaymentReportService {

    public byte[] generateReceiptPdf(Payment payment) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDType1Font titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font labelFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font bodyFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float y = 770;
                content.beginText();
                content.setFont(titleFont, 20);
                content.newLineAtOffset(50, y);
                content.showText("EduChess Payment Receipt");
                content.endText();

                y -= 42;
                y = row(content, labelFont, bodyFont, "Payment ID", String.valueOf(payment.getId()), y);
                y = row(content, labelFont, bodyFont, "Student Name", value(payment.getStudentName()), y);
                y = row(content, labelFont, bodyFont, "Month", value(payment.getMonth()), y);
                y = row(content, labelFont, bodyFont, "Session Count", String.valueOf(payment.getSessionCount()), y);
                y = row(content, labelFont, bodyFont, "Total Amount", "RM " + String.format("%.2f", payment.getTotalAmount() != null ? payment.getTotalAmount() : 0.0), y);
                y = row(content, labelFont, bodyFont, "Payment Status", value(payment.getStatus()), y);
                y = row(content, labelFont, bodyFont, "Paid Date", value(payment.getPaidAt()), y);
                y = row(content, labelFont, bodyFont, "Stripe Session ID", value(payment.getStripeCheckoutSessionId()), y);
                row(content, labelFont, bodyFont, "Stripe Payment Intent ID", value(payment.getStripePaymentIntentId()), y);
            }

            document.save(out);
            return out.toByteArray();
        }
    }

    private float row(PDPageContentStream content,
                      PDType1Font labelFont,
                      PDType1Font bodyFont,
                      String label,
                      String value,
                      float y) throws IOException {
        content.beginText();
        content.setFont(labelFont, 11);
        content.newLineAtOffset(50, y);
        content.showText(label + ":");
        content.endText();

        content.beginText();
        content.setFont(bodyFont, 11);
        content.newLineAtOffset(210, y);
        content.showText(safePdfText(value));
        content.endText();

        return y - 26;
    }

    private String value(Object value) {
        return value == null || value.toString().isBlank() ? "-" : value.toString();
    }

    private String safePdfText(String value) {
        return value(value).replace("\r", " ").replace("\n", " ");
    }
}
