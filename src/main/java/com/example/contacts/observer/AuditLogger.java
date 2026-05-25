package com.example.contacts.observer;

import com.example.contacts.command.Command;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Audit logger: records all executed commands in JSON Lines format.
 *
 * Improvements (see Architectural Review):
 *  - Issue 8.3: Plain-text format replaced with JSON Lines (one JSON object per line).
 *               This makes log entries machine-parseable without an external library.
 *  - Issue 8.2: Size-based log rotation — when the log file exceeds {@code maxLogBytes}
 *               the current file is renamed to {@code audit.log.1} and a fresh file is started.
 *  - Issue 8.1: In the SQLite design, audit is called after {@code repo.save()} returns,
 *               so the log never records an operation that failed to persist.
 *
 * Example entry:
 * <pre>
 * {"timestamp":"2026-05-25T18:00:00","action":"EXECUTE","command":"CreateContactCommand","detail":"Creating a contact John Doe"}
 * </pre>
 */
public final class AuditLogger {

    /** Default rotation threshold: 10 MB */
    public static final long DEFAULT_MAX_LOG_BYTES = 10L * 1024 * 1024;

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final Path logPath;
    private final long maxLogBytes;
    private final boolean alsoConsole;

    /**
     * @param logPath      path to {@code audit.log}
     * @param maxLogBytes  rotate when file exceeds this size in bytes
     * @param alsoConsole  mirror each entry to stdout
     */
    public AuditLogger(Path logPath, long maxLogBytes, boolean alsoConsole) {
        this.logPath      = logPath;
        this.maxLogBytes  = maxLogBytes;
        this.alsoConsole  = alsoConsole;
    }

    /** Convenience constructor that uses the default 10 MB rotation threshold. */
    public AuditLogger(Path logPath, boolean alsoConsole) {
        this(logPath, DEFAULT_MAX_LOG_BYTES, alsoConsole);
    }

    /**
     * Appends a JSON Lines entry for the given command action.
     * Rotates the log file first if it has grown beyond {@code maxLogBytes}.
     */
    public void log(Command cmd, String action) {
        rotateIfNeeded();
        String entry = buildJsonEntry(cmd, action);
        try {
            Files.writeString(
                    logPath,
                    entry + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            System.err.println("Audit log write error: " + e.getMessage());
        }
        if (alsoConsole) {
            System.out.println("[AUDIT] " + entry);
        }
    }

    // ---------- private helpers ----------

    /**
     * Builds a single-line JSON object without an external JSON library.
     * Field ordering: timestamp → action → command → detail.
     */
    private String buildJsonEntry(Command cmd, String action) {
        String timestamp = LocalDateTime.now().format(ISO);
        String cmdName   = cmd.getClass().getSimpleName();
        String detail    = escapeJson(cmd.description());
        return "{"
                + "\"timestamp\":\"" + timestamp    + "\","
                + "\"action\":\""    + action        + "\","
                + "\"command\":\""   + cmdName       + "\","
                + "\"detail\":\""    + detail        + "\""
                + "}";
    }

    /**
     * Minimal JSON string escaping: backslash, double-quote, and common control chars.
     */
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Renames {@code audit.log} → {@code audit.log.1} (overwriting any previous backup)
     * when the file size exceeds the configured threshold.
     */
    private void rotateIfNeeded() {
        try {
            if (Files.exists(logPath) && Files.size(logPath) >= maxLogBytes) {
                Path backup = logPath.resolveSibling(logPath.getFileName() + ".1");
                Files.move(logPath, backup,
                           java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("Audit log rotation error: " + e.getMessage());
        }
    }
}
