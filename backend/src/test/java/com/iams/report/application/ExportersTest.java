package com.iams.report.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/**
 * US-RPT-12: both binary exporters are verified by ROUND-TRIP - the file is
 * parsed back with the same libraries and its content asserted, not just
 * "bytes came out".
 */
class ExportersTest {

    private static TabularReport report(int rows) {
        return new TabularReport("test-report", "Test Report", Instant.parse("2026-07-16T00:00:00Z"),
                List.of("Asset Number", "Name", "Cost"),
                java.util.stream.IntStream.range(0, rows)
                        .mapToObj(i -> List.of("AST-" + String.format("%06d", i), "Asset with, comma \"" + i + '"',
                                "123.4" + i))
                        .toList());
    }

    @Test
    void xlsx_roundTripsHeaderAndCellsAsText() throws IOException {
        byte[] bytes = new XlsxExporter().export(report(3));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            XSSFSheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("Test Report");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Asset Number");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("AST-000000");
            // Leading-zero-preserving text cells, never coerced numbers (the exporter's stated contract)
            assertThat(sheet.getRow(1).getCell(2).getCellType()).isEqualTo(org.apache.poi.ss.usermodel.CellType.STRING);
            assertThat(sheet.getRow(3).getCell(1).getStringCellValue()).contains("comma");
            assertThat(sheet.getPaneInformation().getHorizontalSplitPosition()).isEqualTo((short) 1);
        }
    }

    @Test
    void xlsx_survivesHostileSheetTitle() throws IOException {
        TabularReport hostile = new TabularReport("k", "Bad/Name*With[Illegal]:Chars?And\\More-Longer-Than-31-Chars",
                Instant.now(), List.of("A"), List.of());
        byte[] bytes = new XlsxExporter().export(hostile);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getSheetAt(0).getSheetName()).hasSizeLessThanOrEqualTo(31);
        }
    }

    @Test
    void pdf_paginatesAndRepeatsHeaderWithTitleOnFirstPage() throws IOException {
        byte[] bytes = new PdfExporter().export(report(120));

        try (PDDocument document = Loader.loadPDF(bytes)) {
            assertThat(document.getNumberOfPages()).isGreaterThan(1);
            String allText = new PDFTextStripper().getText(document);
            assertThat(allText).contains("Test Report").contains("AST-000000").contains("AST-000119");
            // Header repeats on every page
            assertThat(allText.split("Asset Number").length - 1).isEqualTo(document.getNumberOfPages());
        }
    }

    @Test
    void pdf_handlesEmptyReportWithTitlePage() throws IOException {
        byte[] bytes = new PdfExporter().export(report(0));
        try (PDDocument document = Loader.loadPDF(bytes)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            assertThat(new PDFTextStripper().getText(document)).contains("Test Report").contains("0 row(s)");
        }
    }
}
