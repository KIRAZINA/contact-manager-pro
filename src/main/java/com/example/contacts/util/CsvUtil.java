package com.example.contacts.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimalistic CSV utility: escaping commas, quotation marks, and line breaks.
 * Parsing simple CSV without nested quotes inside quotes (sufficient for our purposes).
 */
public final class CsvUtil {

    private CsvUtil() {}

    public static String escape(String s) {
        if (s == null) return "";
        boolean needsQuotes = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        String v = s.replace("\"", "\"\"");
        return needsQuotes ? "\"" + v + "\"" : v;
    }

    public static String joinEscaped(List<String> cells) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(escape(cells.get(i)));
        }
        return sb.toString();
    }

    // Simple CSV string parser into cells (supports quotes and commas)
    public static List<String> parseLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cell.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cell.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    out.add(cell.toString());
                    cell.setLength(0);
                } else {
                    cell.append(c);
                }
            }
        }
        out.add(cell.toString());
        return out;
    }
}
