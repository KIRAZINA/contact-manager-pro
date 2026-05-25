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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the refactored AuditLogger (JSON Lines format + size-based rotation).
 */
class AuditLoggerTest {

    @TempDir
    Path tempDir;

    private ContactRepository repo;
    private Command createCommand;
    private Path auditFile;

    @BeforeEach
    void setup() {
        Path testCsv = tempDir.resolve("audit-test.csv");
        repo = new FileContactRepository(testCsv);

        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null);
        createCommand = new CreateContactCommand(repo, c);

        auditFile = tempDir.resolve("audit.log");
    }

    @Test
    void logShouldWriteJsonLineToFile() throws IOException {
        AuditLogger logger = new AuditLogger(auditFile, false);

        logger.log(createCommand, "EXECUTE");

        String content = Files.readString(auditFile);
        assertTrue(content.contains("\"action\":\"EXECUTE\""), "Entry must contain action field");
        assertTrue(content.contains("\"command\":\"CreateContactCommand\""), "Entry must contain command name");
    }

    @Test
    void logShouldIncludeTimestampInIsoFormat() throws IOException {
        AuditLogger logger = new AuditLogger(auditFile, false);

        logger.log(createCommand, "EXECUTE");

        String content = Files.readString(auditFile);
        // ISO format: yyyy-MM-ddTHH:mm:ss
        java.util.regex.Pattern pattern =
                java.util.regex.Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}");
        assertTrue(pattern.matcher(content).find(),
                "Expected ISO timestamp in output: " + content);
    }

    @Test
    void logShouldIncludeCommandDescription() throws IOException {
        AuditLogger logger = new AuditLogger(auditFile, false);

        logger.log(createCommand, "EXECUTE");

        String content = Files.readString(auditFile);
        assertTrue(content.contains("Ivan Petrenko"), "Entry must contain contact name from description");
    }

    @Test
    void logShouldNotThrowWhenAlsoConsoleIsTrue() {
        AuditLogger logger = new AuditLogger(auditFile, true);
        assertDoesNotThrow(() -> logger.log(createCommand, "EXECUTE"));
    }

    @Test
    void multipleLogCallsShouldProduceOneLineEach() throws IOException {
        AuditLogger logger = new AuditLogger(auditFile, false);

        logger.log(createCommand, "EXECUTE");
        logger.log(createCommand, "UNDO");

        List<String> lines = Files.readAllLines(auditFile);
        // Filter out any blank trailing lines
        long nonBlank = lines.stream().filter(l -> !l.isBlank()).count();
        assertEquals(2, nonBlank, "Expected 2 non-blank JSON lines");
        assertTrue(lines.get(0).contains("EXECUTE"));
        assertTrue(lines.get(1).contains("UNDO"));
    }

    @Test
    void logShouldRotateWhenFileSizeExceedsThreshold() throws IOException {
        // Use a threshold of 1 byte so the very first entry triggers rotation
        AuditLogger logger = new AuditLogger(auditFile, 1L, false);

        // First write creates the file (no rotation yet — file doesn't exist)
        logger.log(createCommand, "EXECUTE");
        assertTrue(Files.exists(auditFile));

        // Second write: file is now >= 1 byte → rotate then write fresh entry
        logger.log(createCommand, "UNDO");

        Path backup = auditFile.resolveSibling("audit.log.1");
        assertTrue(Files.exists(backup), "Backup file audit.log.1 must exist after rotation");

        String newContent = Files.readString(auditFile);
        assertTrue(newContent.contains("UNDO"), "New log file must contain the post-rotation entry");
    }
}
