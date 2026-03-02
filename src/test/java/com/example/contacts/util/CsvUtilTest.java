package com.example.contacts.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CsvUtil - CSV parsing and serialization.
 */
class CsvUtilTest {

    @Test
    void escapeShouldAddQuotesWhenContainsComma() {
        String result = CsvUtil.escape("Hello, World");
        assertEquals("\"Hello, World\"", result);
    }

    @Test
    void escapeShouldAddQuotesWhenContainsQuote() {
        String result = CsvUtil.escape("He said \"hello\"");
        assertEquals("\"He said \"\"hello\"\"\"", result);
    }

    @Test
    void escapeShouldAddQuotesWhenContainsNewline() {
        String result = CsvUtil.escape("Line1\nLine2");
        assertEquals("\"Line1\nLine2\"", result);
    }

    @Test
    void escapeShouldNotAddQuotesForSimpleString() {
        String result = CsvUtil.escape("Hello");
        assertEquals("Hello", result);
    }

    @Test
    void escapeShouldHandleNull() {
        String result = CsvUtil.escape(null);
        assertEquals("", result);
    }

    @Test
    void escapeShouldHandleEmptyString() {
        String result = CsvUtil.escape("");
        assertEquals("", result);
    }

    @Test
    void joinEscapedShouldJoinWithCommas() {
        List<String> cells = Arrays.asList("A", "B", "C");
        String result = CsvUtil.joinEscaped(cells);
        assertEquals("A,B,C", result);
    }

    @Test
    void joinEscapedShouldEscapeCellsWithCommas() {
        List<String> cells = Arrays.asList("A", "B,C", "D");
        String result = CsvUtil.joinEscaped(cells);
        assertEquals("A,\"B,C\",D", result);
    }

    @Test
    void parseLineShouldSplitByComma() {
        List<String> result = CsvUtil.parseLine("A,B,C");
        assertEquals(Arrays.asList("A", "B", "C"), result);
    }

    @Test
    void parseLineShouldHandleQuotedFields() {
        List<String> result = CsvUtil.parseLine("A,\"B,C\",D");
        assertEquals(Arrays.asList("A", "B,C", "D"), result);
    }

    @Test
    void parseLineShouldHandleEscapedQuotes() {
        List<String> result = CsvUtil.parseLine("A,\"He said \"\"hi\"\"\",B");
        assertEquals(Arrays.asList("A", "He said \"hi\"", "B"), result);
    }

    @Test
    void parseLineShouldHandleEmptyFields() {
        List<String> result = CsvUtil.parseLine("A,,B");
        assertEquals(Arrays.asList("A", "", "B"), result);
    }

    @Test
    void parseLineShouldHandleTrailingComma() {
        List<String> result = CsvUtil.parseLine("A,B,");
        assertEquals(Arrays.asList("A", "B", ""), result);
    }

    @Test
    void parseLineShouldHandleLeadingComma() {
        List<String> result = CsvUtil.parseLine(",A,B");
        assertEquals(Arrays.asList("", "A", "B"), result);
    }
}
