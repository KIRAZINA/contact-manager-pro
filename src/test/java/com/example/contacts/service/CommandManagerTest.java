package com.example.contacts.service;

import com.example.contacts.command.CreateContactCommand;
import com.example.contacts.domain.entity.Contact;
import com.example.contacts.domain.value.Email;
import com.example.contacts.domain.value.PhoneNumber;
import com.example.contacts.repository.ContactRepository;
import com.example.contacts.repository.FileContactRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommandManagerTest {

    private ContactRepository repo;
    private CommandManager manager;

    @BeforeEach
    void setup() {
        // Test file in target/test-contacts.csv
        Path testFile = Path.of("target", "test-contacts.csv");
        repo = new FileContactRepository(testFile);
        manager = new CommandManager();
    }

    @Test
    void executeAndUndoCreateContact() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null);

        CreateContactCommand cmd = new CreateContactCommand(repo, c);

        manager.executeCommand(cmd);
        assertTrue(repo.findById(c.getId()).isPresent(), "Contact should be created");

        manager.undo();
        assertFalse(repo.findById(c.getId()).isPresent(), "Contact should be deleted after undo");
    }

    @Test
    void redoAfterUndoShouldRestoreContact() {
        Contact c = Contact.createNew("Petro", "Ivanenko",
                List.of(new PhoneNumber("0509876543")),
                List.of(new Email("petro@test.com")), null);

        CreateContactCommand cmd = new CreateContactCommand(repo, c);

        manager.executeCommand(cmd);
        manager.undo();
        assertFalse(repo.findById(c.getId()).isPresent());

        manager.redo();
        assertTrue(repo.findById(c.getId()).isPresent(), "Contact should be restored after redo");
    }

    @Test
    void redoStackShouldClearAfterNewCommand() {
        Contact c1 = Contact.createNew("Anna", "Koval",
                List.of(new PhoneNumber("0931112233")),
                List.of(new Email("anna@test.com")), null);

        Contact c2 = Contact.createNew("Oleh", "Bondar",
                List.of(new PhoneNumber("0934445566")),
                List.of(new Email("oleh@test.com")), null);

        CreateContactCommand cmd1 = new CreateContactCommand(repo, c1);
        CreateContactCommand cmd2 = new CreateContactCommand(repo, c2);

        manager.executeCommand(cmd1);
        manager.undo();
        assertTrue(manager.canRedo());

        // Executing a new command should clear the redo stack
        manager.executeCommand(cmd2);
        assertFalse(manager.canRedo(), "Redo stack should be cleared after new command");
    }
}
