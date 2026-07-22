package com.iams.migration.application;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * RFC 4180 CSV reader - the counterpart to {@link com.iams.report.application.CsvExporter}
 * (which only writes). Handles quoted fields with embedded commas, doubled quotes,
 * and embedded newlines, plus a leading UTF-8 BOM (Excel writes one). Returns every
 * physical record as a list of cells, header row included; the caller maps columns.
 */
@Component
public class CsvParser {

    /** Parse CSV bytes into rows of cells. A trailing empty line is ignored. */
    public List<List<String>> parse(byte[] content) {
        String text = new String(content, StandardCharsets.UTF_8);
        if (text.startsWith("﻿")) {
            text = text.substring(1); // strip UTF-8 BOM
        }
        List<List<String>> rows = new ArrayList<>();
        List<String> current = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;
        boolean rowHasContent = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        cell.append('"'); // doubled quote -> literal quote
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cell.append(c);
                }
                continue;
            }
            switch (c) {
                case '"' -> inQuotes = true;
                case ',' -> {
                    current.add(cell.toString());
                    cell.setLength(0);
                    rowHasContent = true;
                }
                case '\r' -> { /* swallow; the paired \n (or a lone \r) ends the row */ }
                case '\n' -> {
                    current.add(cell.toString());
                    cell.setLength(0);
                    rows.add(current);
                    current = new ArrayList<>();
                    rowHasContent = false;
                }
                default -> {
                    cell.append(c);
                    rowHasContent = true;
                }
            }
        }
        // Flush the final record if the file didn't end with a newline.
        if (cell.length() > 0 || rowHasContent || !current.isEmpty()) {
            current.add(cell.toString());
            rows.add(current);
        }
        return rows;
    }
}
