package com.iams.report.application;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

/**
 * PDF rendering for any TabularReport (US-RPT-12's PDF format): landscape A4,
 * repeated header row per page, equal column split, cell text truncated with
 * an ellipsis rather than overflowing into the neighbour column - a printable
 * artifact of the on-screen table, not a layout engine. pdfbox was already on
 * the classpath for label rendering; no new dependency.
 */
@Component
public class PdfExporter {

    private static final PDRectangle PAGE = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
    private static final float MARGIN = 36f;
    private static final float TITLE_SIZE = 14f;
    private static final float CELL_SIZE = 8f;
    private static final float ROW_HEIGHT = 14f;
    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    private static final PDFont FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    public byte[] export(TabularReport report) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            float tableWidth = PAGE.getWidth() - 2 * MARGIN;
            float colWidth = tableWidth / Math.max(1, report.columns().size());
            float bottomLimit = MARGIN + ROW_HEIGHT; // keep room for the page footer

            int rowIndex = 0;
            int pageNumber = 0;
            List<List<String>> rows = report.rows();
            while (rowIndex < rows.size() || pageNumber == 0) {
                pageNumber++;
                PDPage page = new PDPage(PAGE);
                document.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                    float y = PAGE.getHeight() - MARGIN;
                    if (pageNumber == 1) {
                        writeLine(cs, FONT_BOLD, TITLE_SIZE, MARGIN, y, report.title());
                        y -= TITLE_SIZE + 4;
                        writeLine(cs, FONT, CELL_SIZE, MARGIN, y,
                                "Generated " + STAMP.format(report.generatedAt()) + " - " + rows.size() + " row(s)");
                        y -= ROW_HEIGHT + 4;
                    }
                    // Header row, repeated on every page
                    for (int c = 0; c < report.columns().size(); c++) {
                        writeLine(cs, FONT_BOLD, CELL_SIZE, MARGIN + c * colWidth, y,
                                fit(report.columns().get(c), colWidth));
                    }
                    y -= ROW_HEIGHT;
                    while (rowIndex < rows.size() && y > bottomLimit) {
                        List<String> row = rows.get(rowIndex);
                        for (int c = 0; c < row.size() && c < report.columns().size(); c++) {
                            writeLine(cs, FONT, CELL_SIZE, MARGIN + c * colWidth, y, fit(row.get(c), colWidth));
                        }
                        y -= ROW_HEIGHT;
                        rowIndex++;
                    }
                    writeLine(cs, FONT, CELL_SIZE, MARGIN, MARGIN / 2, "Page " + pageNumber);
                }
                if (rows.isEmpty()) {
                    break;
                }
            }
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed rendering pdf for report " + report.key(), e);
        }
    }

    private static void writeLine(PDPageContentStream cs, PDFont font, float size, float x, float y, String text)
            throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitize(text));
        cs.endText();
    }

    /** Truncate to the column width with an ellipsis rather than overlapping the neighbour. */
    private static String fit(String text, float colWidth) {
        if (text == null) {
            return "";
        }
        // Helvetica at 8pt averages ~4.5pt/char; keep a 4pt gutter.
        int maxChars = Math.max(3, (int) ((colWidth - 4) / 4.5f));
        return text.length() <= maxChars ? text : text.substring(0, maxChars - 1) + "…";
    }

    /** Standard-14 fonts are WinAnsi-only; replace anything unencodable rather than throwing mid-render. */
    private static String sanitize(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (char ch : text.toCharArray()) {
            if (ch == '\n' || ch == '\r' || ch == '\t') {
                sb.append(' ');
            } else if (ch >= 32 && ch <= 126 || ch >= 160 && ch <= 255 || ch == '…') {
                sb.append(ch);
            } else {
                sb.append('?');
            }
        }
        return sb.toString();
    }
}
