package com.iams.report.application;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * .xlsx rendering for any TabularReport (US-RPT-12's Excel format). Same
 * contract as CsvExporter: what's on screen is what exports - all cells are
 * written as text, deliberately, because these reports are already
 * string-shaped by design (TabularReport's own trade-off) and silently
 * coercing "000123" or "1/2" into Excel numbers/dates would corrupt exactly
 * the asset numbers and codes this system exists to keep straight.
 */
@Component
public class XlsxExporter {

    /** Excel's hard cap; a longer title degrades to the generic name rather than failing the export. */
    private static final int MAX_SHEET_NAME = 31;

    public byte[] export(TabularReport report) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(safeSheetName(report.title()));

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);

            Row header = sheet.createRow(0);
            for (int c = 0; c < report.columns().size(); c++) {
                Cell cell = header.createCell(c);
                cell.setCellValue(report.columns().get(c));
                cell.setCellStyle(headerStyle);
            }
            int r = 1;
            for (List<String> rowData : report.rows()) {
                Row row = sheet.createRow(r++);
                for (int c = 0; c < rowData.size(); c++) {
                    row.createCell(c).setCellValue(rowData.get(c) == null ? "" : rowData.get(c));
                }
            }
            for (int c = 0; c < report.columns().size(); c++) {
                sheet.autoSizeColumn(c);
            }
            sheet.createFreezePane(0, 1);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed rendering xlsx for report " + report.key(), e);
        }
    }

    private static String safeSheetName(String title) {
        if (title == null || title.isBlank()) {
            return "Report";
        }
        // POI rejects the characters Excel itself forbids in sheet names.
        String cleaned = title.replaceAll("[\\\\/*?\\[\\]:]", " ").trim();
        if (cleaned.isEmpty()) {
            return "Report";
        }
        return cleaned.length() > MAX_SHEET_NAME ? cleaned.substring(0, MAX_SHEET_NAME) : cleaned;
    }
}
