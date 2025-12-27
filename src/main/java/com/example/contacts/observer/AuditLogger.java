package com.example.contacts.observer;

import com.example.contacts.command.Command;

import java.io.IOException;
import java.io.Writer;
import java.time.LocalDateTime;

/**
 * A simple Observer for logging executed commands.
 * Can write to the console or to a file (via Writer).
 */
public final class AuditLogger {

    private final Writer writer;
    private final boolean alsoConsole;

    public AuditLogger(Writer writer, boolean alsoConsole) {
        this.writer = writer;
        this.alsoConsole = alsoConsole;
    }

    public void log(Command cmd, String action) {
        String entry = LocalDateTime.now() + " | " + action + " | " + cmd.description();
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
