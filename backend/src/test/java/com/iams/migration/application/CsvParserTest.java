package com.iams.migration.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class CsvParserTest {

    private final CsvParser parser = new CsvParser();

    private List<List<String>> parse(String csv) {
        return parser.parse(csv.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void parsesSimpleRows() {
        List<List<String>> rows = parse("name,code\nDell,IT-1\nHP,IT-2\n");
        assertThat(rows).hasSize(3);
        assertThat(rows.get(0)).containsExactly("name", "code");
        assertThat(rows.get(1)).containsExactly("Dell", "IT-1");
        assertThat(rows.get(2)).containsExactly("HP", "IT-2");
    }

    @Test
    void handlesQuotedFieldWithComma() {
        List<List<String>> rows = parse("name,note\n\"Dell, Inc.\",ok\n");
        assertThat(rows.get(1)).containsExactly("Dell, Inc.", "ok");
    }

    @Test
    void handlesDoubledQuoteInsideQuotedField() {
        List<List<String>> rows = parse("name\n\"a \"\"quoted\"\" word\"\n");
        assertThat(rows.get(1)).containsExactly("a \"quoted\" word");
    }

    @Test
    void handlesEmbeddedNewlineInsideQuotedField() {
        List<List<String>> rows = parse("name\n\"line1\nline2\"\n");
        assertThat(rows).hasSize(2);
        assertThat(rows.get(1)).containsExactly("line1\nline2");
    }

    @Test
    void stripsUtf8Bom() {
        byte[] withBom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'n', 'a', 'm', 'e'};
        List<List<String>> rows = parser.parse(withBom);
        assertThat(rows.get(0)).containsExactly("name");
    }

    @Test
    void parsesFinalRowWithoutTrailingNewline() {
        List<List<String>> rows = parse("a,b\nc,d");
        assertThat(rows).hasSize(2);
        assertThat(rows.get(1)).containsExactly("c", "d");
    }

    @Test
    void keepsEmptyCells() {
        List<List<String>> rows = parse("a,b,c\n1,,3\n");
        assertThat(rows.get(1)).containsExactly("1", "", "3");
    }

    @Test
    void handlesCrlfLineEndings() {
        List<List<String>> rows = parse("a,b\r\n1,2\r\n");
        assertThat(rows).hasSize(2);
        assertThat(rows.get(1)).containsExactly("1", "2");
    }
}
