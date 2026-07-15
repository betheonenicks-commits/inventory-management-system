package com.iams.report.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class CsvExporterTest {

    private final CsvExporter exporter = new CsvExporter();

    private String exportAsString(List<String> columns, List<List<String>> rows) {
        byte[] out = exporter.export(new TabularReport("t", "T", Instant.now(), columns, rows));
        // Strip the UTF-8 BOM for assertions.
        return new String(out, 3, out.length - 3, StandardCharsets.UTF_8);
    }

    @Test
    void startsWithUtf8Bom() {
        byte[] out = exporter.export(new TabularReport("t", "T", Instant.now(), List.of("A"), List.of()));
        assertThat(out[0]).isEqualTo((byte) 0xEF);
        assertThat(out[1]).isEqualTo((byte) 0xBB);
        assertThat(out[2]).isEqualTo((byte) 0xBF);
    }

    @Test
    void plainCellsRenderUnquotedWithCrlf() {
        String csv = exportAsString(List.of("A", "B"), List.of(List.of("1", "2")));
        assertThat(csv).isEqualTo("A,B\r\n1,2\r\n");
    }

    @Test
    void commasQuotesAndNewlinesAreQuotedAndDoubled() {
        String csv = exportAsString(List.of("Name"), List.of(
                List.of("Laptop, 15\""),
                List.of("line1\nline2")));
        assertThat(csv).isEqualTo("Name\r\n\"Laptop, 15\"\"\"\r\n\"line1\nline2\"\r\n");
    }

    @Test
    void nullCellRendersEmpty() {
        String csv = exportAsString(List.of("A", "B"), List.of(java.util.Arrays.asList(null, "x")));
        assertThat(csv).isEqualTo("A,B\r\n,x\r\n");
    }
}
