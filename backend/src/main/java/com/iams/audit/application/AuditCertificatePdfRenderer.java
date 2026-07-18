package com.iams.audit.application;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

/**
 * US-AUD-15: the completion certificate as a formal, downloadable document.
 * Deliberately NOT the table-shaped PdfExporter: a certificate is a
 * document layout (centered single page, statement text, summary block,
 * approver and date), not a printable table. Same pdfbox Standard-14
 * constraints as the exporter: WinAnsi-only text, so unencodable
 * characters are replaced rather than thrown mid-render.
 */
@Component
public class AuditCertificatePdfRenderer {

    private static final PDRectangle PAGE = PDRectangle.A4;
    private static final float MARGIN = 64f;
    private static final DateTimeFormatter DATE_STAMP =
            DateTimeFormatter.ofPattern("d MMMM yyyy").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter GENERATED_STAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    private static final PDFont FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    public byte[] render(AuditReportService.AuditCertificate certificate) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PAGE);
            document.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float width = PAGE.getWidth();
                float y = PAGE.getHeight() - MARGIN - 40;

                centered(cs, FONT, 11, width, y, "IAMS — Inventory Asset Management System");
                y -= 42;
                centered(cs, FONT_BOLD, 24, width, y, "Audit Completion Certificate");
                y -= 18;
                rule(cs, width / 2 - 90, y, 180);
                y -= 48;

                centered(cs, FONT, 12, width, y, "This certifies that the audit");
                y -= 30;
                centered(cs, FONT_BOLD, 17, width, y, certificate.auditName());
                y -= 30;
                centered(cs, FONT, 12, width, y, "was completed, approved, and closed with the following outcome:");
                y -= 56;

                float labelX = width / 2 - 110;
                float valueX = width / 2 + 40;
                y = statLine(cs, labelX, valueX, y, "Assets in scope", certificate.expectedCount());
                y = statLine(cs, labelX, valueX, y, "Verified", certificate.verifiedCount());
                y = statLine(cs, labelX, valueX, y, "Missing", certificate.missingCount());
                y = statLine(cs, labelX, valueX, y, "Damaged", certificate.damagedCount());
                y -= 40;

                String approver = certificate.approverName() != null ? certificate.approverName() : "(not recorded)";
                centered(cs, FONT, 12, width, y, "Approved by " + approver
                        + (certificate.approvedAt() != null ? " on " + DATE_STAMP.format(certificate.approvedAt()) : ""));

                float footerY = MARGIN;
                centered(cs, FONT, 8, width, footerY + 12,
                        "Certificate generated " + GENERATED_STAMP.format(Instant.now()));
                centered(cs, FONT, 8, width, footerY, "Audit reference " + certificate.auditId());
            }
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed rendering certificate for audit " + certificate.auditId(), e);
        }
    }

    private static float statLine(PDPageContentStream cs, float labelX, float valueX, float y,
                                  String label, long value) throws IOException {
        text(cs, FONT, 12, labelX, y, label);
        text(cs, FONT_BOLD, 12, valueX, y, String.valueOf(value));
        return y - 22;
    }

    private static void centered(PDPageContentStream cs, PDFont font, float size, float pageWidth, float y,
                                 String value) throws IOException {
        String safe = sanitize(value);
        float textWidth = font.getStringWidth(safe) / 1000f * size;
        text(cs, font, size, (pageWidth - textWidth) / 2, y, safe);
    }

    private static void text(PDPageContentStream cs, PDFont font, float size, float x, float y, String value)
            throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitize(value));
        cs.endText();
    }

    private static void rule(PDPageContentStream cs, float x, float y, float length) throws IOException {
        cs.moveTo(x, y);
        cs.lineTo(x + length, y);
        cs.setLineWidth(0.8f);
        cs.stroke();
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (char ch : value.toCharArray()) {
            if (ch == '\n' || ch == '\r' || ch == '\t') {
                sb.append(' ');
            } else if (ch >= 32 && ch <= 126 || ch >= 160 && ch <= 255) {
                sb.append(ch);
            } else {
                sb.append('?');
            }
        }
        return sb.toString();
    }
}
