package com.iams.report.application;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * RFC 4180 CSV rendering for any TabularReport (US-RPT-12's CSV format).
 * UTF-8 with a BOM - Excel misreads BOM-less UTF-8 CSVs as the legacy
 * codepage, silently mangling any non-ASCII asset name, which for a report
 * a Viewer hands to a board meeting is a correctness bug, not cosmetics.
 */
@Component
public class CsvExporter {

    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    public byte[] export(TabularReport report) {
        StringBuilder sb = new StringBuilder();
        appendRow(sb, report.columns());
        for (List<String> row : report.rows()) {
            appendRow(sb, row);
        }
        byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[UTF8_BOM.length + body.length];
        System.arraycopy(UTF8_BOM, 0, out, 0, UTF8_BOM.length);
        System.arraycopy(body, 0, out, UTF8_BOM.length, body.length);
        return out;
    }

    private void appendRow(StringBuilder sb, List<String> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(cells.get(i)));
        }
        sb.append("\r\n");
    }

    /** RFC 4180: quote any cell containing a comma, quote, or line break; double embedded quotes. */
    private String escape(String cell) {
        if (cell == null) {
            return "";
        }
        boolean needsQuoting = cell.contains(",") || cell.contains("\"") || cell.contains("\n") || cell.contains("\r");
        return needsQuoting ? '"' + cell.replace("\"", "\"\"") + '"' : cell;
    }
}
