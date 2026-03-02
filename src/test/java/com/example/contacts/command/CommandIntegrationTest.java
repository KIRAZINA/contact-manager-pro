package com.example.contacts.command;

import com.example.contacts.domain.entity.Contact;
import com.example.contacts.domain.enum_.ContactStatus;
import com.example.contacts.domain.value.Email;
import com.example.contacts.domain.value.PhoneNumber;
import com.example.contacts.repository.ContactRepository;
import com.example.contacts.repository.FileContactRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Command implementations.
 */
class CommandIntegrationTest {

    @TempDir
    Path tempDir;
    
    private ContactRepository repo;

    @BeforeEach
    void setup() {
        Path testFile = tempDir.resolve("commands-test.csv");
        repo = new FileContactRepository(testFile);
    }

    // ========== UpdateContactCommand ==========

    @Test
    void updateCommandShouldSaveOldStateForUndo() {
        Contact original = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null);
        repo.save(original);

        Contact updated = new Contact(original.getId(), "Ivan", "Petrenko-Updated",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null,
                original.getCreatedAt(), java.time.LocalDateTime.now(), ContactStatus.ACTIVE);

        UpdateContactCommand cmd = new UpdateContactCommand(repo, updated);
        cmd.execute();

        Contact fromRepo = repo.findById(original.getId()).orElseThrow();
        assertEquals("Petrenko-Updated", fromRepo.getLastName());
    }

    @Test
    void updateCommandUndoShouldRestoreOldState() {
        Contact original = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null);
        repo.save(original);

        Contact updated = new Contact(original.getId(), "Ivan", "Petrenko-Updated",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null,
                original.getCreatedAt(), java.time.LocalDateTime.now(), ContactStatus.ACTIVE);

        UpdateContactCommand cmd = new UpdateContactCommand(repo, updated);
        cmd.execute();
        cmd.undo();

        Contact fromRepo = repo.findById(original.getId()).orElseThrow();
        assertEquals("Petrenko", fromRepo.getLastName());
    }

    // ========== DeleteContactCommand ==========

    @Test
    void deleteCommandShouldRemoveContact() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null);
        repo.save(c);

        DeleteContactCommand cmd = new DeleteContactCommand(repo, c);
        cmd.execute();

        assertFalse(repo.findById(c.getId()).isPresent());
    }

    @Test
    void deleteCommandUndoShouldRestoreContact() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null);
        repo.save(c);

        DeleteContactCommand cmd = new DeleteContactCommand(repo, c);
        cmd.execute();
        cmd.undo();

        Contact restored = repo.findById(c.getId()).orElseThrow();
        assertEquals("Ivan", restored.getFirstName());
    }

    // ========== ArchiveContactCommand ==========

    @Test
    void archiveCommandShouldChangeStatusToArchived() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null);
        repo.save(c);

        ArchiveContactCommand cmd = new ArchiveContactCommand(repo, c);
        cmd.execute();

        Contact archived = repo.findById(c.getId()).orElseThrow();
        assertEquals(ContactStatus.ARCHIVED, archived.getStatus());
    }

    @Test
    void archiveCommandUndoShouldRestoreToActive() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null);
        repo.save(c);

        ArchiveContactCommand cmd = new ArchiveContactCommand(repo, c);
        cmd.execute();
        cmd.undo();

        Contact restored = repo.findById(c.getId()).orElseThrow();
        assertEquals(ContactStatus.ACTIVE, restored.getStatus());
    }

    @Test
    void archiveCommandShouldBeIdempotent() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null);
        repo.save(c);

        ArchiveContactCommand cmd = new ArchiveContactCommand(repo, c);
        cmd.execute();
        cmd.execute(); // Second call should not fail

        Contact archived = repo.findById(c.getId()).orElseThrow();
        assertEquals(ContactStatus.ARCHIVED, archived.getStatus());
    }

    // ========== RestoreContactCommand ==========

    @Test
    void restoreCommandShouldChangeStatusToActive() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null);
        c.archive();
        repo.save(c);

        RestoreContactCommand cmd = new RestoreContactCommand(repo, c);
        cmd.execute();

        Contact restored = repo.findById(c.getId()).orElseThrow();
        assertEquals(ContactStatus.ACTIVE, restored.getStatus());
    }

    @Test
    void restoreCommandUndoShouldArchiveAgain() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null);
        c.archive();
        repo.save(c);

        RestoreContactCommand cmd = new RestoreContactCommand(repo, c);
        cmd.execute();
        cmd.undo();

        Contact archived = repo.findById(c.getId()).orElseThrow();
        assertEquals(ContactStatus.ARCHIVED, archived.getStatus());
    }

    // ========== Command Description ==========

    @Test
    void createCommandShouldHaveDescription() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null);
        CreateContactCommand cmd = new CreateContactCommand(repo, c);

        assertTrue(cmd.description().contains("Ivan"));
        assertTrue(cmd.description().contains("Petrenko"));
    }

    @Test
    void updateCommandShouldHaveDescription() {
        Contact original = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null);
        repo.save(original);

        Contact updated = new Contact(original.getId(), "Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null,
                original.getCreatedAt(), java.time.LocalDateTime.now(), ContactStatus.ACTIVE);

        UpdateContactCommand cmd = new UpdateContactCommand(repo, updated);

        assertTrue(cmd.description().contains(original.getId().toString()));
    }

    @Test
    void deleteCommandShouldHaveDescription() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null);
        DeleteContactCommand cmd = new DeleteContactCommand(repo, c);

        assertTrue(cmd.description().contains(c.getId().toString()));
    }
}
