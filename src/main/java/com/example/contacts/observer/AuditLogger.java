package com.example.contacts.observer;

import com.example.contacts.command.Command;

import java.io.IOException;
import java.io.Writer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Audit logger: records all executed commands with timestamp and description.
 * Can write to the console or to a file (via Writer).
 */
public final class AuditLogger {

    private final Writer writer;
    private final boolean alsoConsole;
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AuditLogger(Writer writer, boolean alsoConsole) {
        this.writer = writer;
        this.alsoConsole = alsoConsole;
    }

    public void log(Command cmd, String action) {
        String timestamp = LocalDateTime.now().format(FORMATTER);

        String entry = timestamp
                + " | ACTION=" + action
                + " | Command=" + cmd.getClass().getSimpleName()
                + " | " + cmd.description();

        try {
            writer.write(entry + System.lineSeparator());
            writer.flush();
        } catch (IOException e) {
            System.err.println("Log entry error: " + e.getMessage());
        }

        if (alsoConsole) {
            System.out.println("[AUDIT] " + entry);
        }
    }
}
