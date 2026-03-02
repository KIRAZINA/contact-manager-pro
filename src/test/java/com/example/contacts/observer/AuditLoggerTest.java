package com.example.contacts.observer;

import com.example.contacts.command.Command;
import com.example.contacts.command.CreateContactCommand;
import com.example.contacts.domain.entity.Contact;
import com.example.contacts.domain.value.Email;
import com.example.contacts.domain.value.PhoneNumber;
import com.example.contacts.repository.ContactRepository;
import com.example.contacts.repository.FileContactRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AuditLogger - Observer pattern implementation.
 */
class AuditLoggerTest {

    @TempDir
    Path tempDir;
    
    private ContactRepository repo;
    private Command createCommand;

    @BeforeEach
    void setup() {
        Path testFile = tempDir.resolve("audit-test.csv");
        repo = new FileContactRepository(testFile);
        
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null);
        createCommand = new CreateContactCommand(repo, c);
    }

    @Test
    void logShouldWriteToWriter() {
        StringWriter writer = new StringWriter();
        AuditLogger logger = new AuditLogger(writer, false);

        logger.log(createCommand, "EXECUTE");

        String output = writer.toString();
        assertTrue(output.contains("EXECUTE"));
        assertTrue(output.contains("CreateContactCommand"));
    }

    @Test
    void logShouldIncludeTimestamp() {
        StringWriter writer = new StringWriter();
        AuditLogger logger = new AuditLogger(writer, false);

        logger.log(createCommand, "EXECUTE");

        String output = writer.toString();
        // Timestamp format: yyyy-MM-dd HH:mm:ss
        // Use Pattern/Matcher for more reliable matching across platforms
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
        java.util.regex.Matcher matcher = pattern.matcher(output);
        assertTrue(matcher.find(), "Expected timestamp in format yyyy-MM-dd HH:mm:ss in output: " + output);
    }

    @Test
    void logShouldIncludeCommandDescription() {
        StringWriter writer = new StringWriter();
        AuditLogger logger = new AuditLogger(writer, false);

        logger.log(createCommand, "EXECUTE");

        String output = writer.toString();
        assertTrue(output.contains("Creating a contact Ivan Petrenko"));
    }

    @Test
    void logShouldWriteToConsoleWhenAlsoConsoleIsTrue() {
        StringWriter writer = new StringWriter();
        AuditLogger logger = new AuditLogger(writer, true);

        // This will print to console but we can't easily test that
        // We just verify it doesn't throw
        assertDoesNotThrow(() -> logger.log(createCommand, "EXECUTE"));
    }

    @Test
    void logMultipleActionsShouldWriteSequentially() {
        StringWriter writer = new StringWriter();
        AuditLogger logger = new AuditLogger(writer, false);

        logger.log(createCommand, "EXECUTE");
        logger.log(createCommand, "UNDO");

        String output = writer.toString();
        String[] lines = output.trim().split("\n");
        assertEquals(2, lines.length);
        assertTrue(lines[0].contains("EXECUTE"));
        assertTrue(lines[1].contains("UNDO"));
    }
}
