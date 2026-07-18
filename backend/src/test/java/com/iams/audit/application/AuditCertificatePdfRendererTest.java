package com.iams.audit.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class AuditCertificatePdfRendererTest {

    private final AuditCertificatePdfRenderer renderer = new AuditCertificatePdfRenderer();

    private static String extractText(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }

    @Test
    void render_producesAOnePagePdfNamingAuditCountsApproverAndDate() throws IOException {
        UUID auditId = UUID.randomUUID();
        byte[] pdf = renderer.render(new AuditReportService.AuditCertificate(
                auditId, "Q3 Library Sweep", 120, 117, 2, 1,
                UUID.randomUUID(), "Dana Head", Instant.parse("2026-07-01T10:15:00Z")));

        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        String text = extractText(pdf);
        assertThat(text).contains("Audit Completion Certificate")
                .contains("Q3 Library Sweep")
                .contains("120").contains("117")
                .contains("Approved by Dana Head on 1 July 2026")
                .contains(auditId.toString());
    }

    @Test
    void render_survivesMissingApproverAndUnencodableCharacters() throws IOException {
        byte[] pdf = renderer.render(new AuditReportService.AuditCertificate(
                UUID.randomUUID(), "Audit — 中文 name", 1, 1, 0, 0, null, null, null));

        String text = extractText(pdf);
        // Unencodable CJK replaced, never a mid-render throw; absent approver stated, not blank.
        assertThat(text).contains("Approved by (not recorded)");
        assertThat(text).contains("? name");
    }
}
