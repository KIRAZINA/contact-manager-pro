package com.example.contacts.service;

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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ContactService - Application Service layer.
 */
class ContactServiceTest {

    @TempDir
    Path tempDir;
    
    private ContactRepository repo;
    private CommandManager commandManager;
    private ContactService service;

    @BeforeEach
    void setup() {
        Path testFile = tempDir.resolve("service-test.csv");
        repo = new FileContactRepository(testFile);
        commandManager = new CommandManager();
        service = new ContactService(repo, commandManager);
    }

    @Test
    void createContactShouldPersistAndReturn() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null);

        Contact result = service.createContact(c);

        assertNotNull(result);
        assertTrue(repo.findById(c.getId()).isPresent());
    }

    @Test
    void findContactShouldReturnOptional() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null);
        repo.save(c);

        var found = service.findContact(c.getId());

        assertTrue(found.isPresent());
        assertEquals("Ivan", found.get().getFirstName());
    }

    @Test
    void listContactsShouldReturnAll() {
        Contact c1 = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")), List.of(), null);
        Contact c2 = Contact.createNew("Petro", "Ivanenko",
                List.of(new PhoneNumber("0509876543")), List.of(), null);
        repo.save(c1);
        repo.save(c2);

        List<Contact> all = service.listContacts();

        assertEquals(2, all.size());
    }

    @Test
    void updateContactShouldSaveNewState() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")), List.of(), null);
        repo.save(c);

        Contact updated = new Contact(c.getId(), "Ivan", "Petrenko-New",
                List.of(new PhoneNumber("0671234567")), List.of(),
                null, c.getCreatedAt(), java.time.LocalDateTime.now(), ContactStatus.ACTIVE);

        service.updateContact(updated);

        Contact fromRepo = repo.findById(c.getId()).orElseThrow();
        assertEquals("Petrenko-New", fromRepo.getLastName());
    }

    @Test
    void deleteContactShouldRemoveFromRepo() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")), List.of(), null);
        repo.save(c);

        service.deleteContact(c);

        assertFalse(repo.findById(c.getId()).isPresent());
    }

    @Test
    void archiveContactShouldChangeStatus() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")), List.of(), null);
        repo.save(c);

        service.archiveContact(c);

        Contact archived = repo.findById(c.getId()).orElseThrow();
        assertEquals(ContactStatus.ARCHIVED, archived.getStatus());
    }

    @Test
    void restoreContactShouldChangeStatusBackToActive() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")), List.of(), null);
        c.archive();
        repo.save(c);

        service.restoreContact(c);

        Contact restored = repo.findById(c.getId()).orElseThrow();
        assertEquals(ContactStatus.ACTIVE, restored.getStatus());
    }

    @Test
    void fullTextSearchShouldFindByName() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null);
        repo.save(c);

        List<Contact> results = service.fullTextSearch("Petrenko");

        assertEquals(1, results.size());
        assertEquals("Petrenko", results.get(0).getLastName());
    }

    @Test
    void fullTextSearchShouldFindByEmail() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null);
        repo.save(c);

        List<Contact> results = service.fullTextSearch("ivan@test");

        assertEquals(1, results.size());
    }

    @Test
    void fullTextSearchShouldBeCaseInsensitive() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("IVAN@TEST.COM")), null);
        repo.save(c);

        List<Contact> results = service.fullTextSearch("ivan");

        assertEquals(1, results.size());
    }

    @Test
    void filterByStatusShouldReturnOnlyMatching() {
        Contact c1 = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")), List.of(), null);
        Contact c2 = Contact.createNew("Petro", "Ivanenko",
                List.of(new PhoneNumber("0509876543")), List.of(), null);
        c2.archive();
        repo.save(c1);
        repo.save(c2);

        List<Contact> active = service.filterByStatus(ContactStatus.ACTIVE);

        assertEquals(1, active.size());
        assertEquals("Petrenko", active.get(0).getLastName());
    }

    @Test
    void undoShouldRevertLastCommand() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")), List.of(), null);

        service.createContact(c);
        assertTrue(service.canUndo());

        service.undo();

        assertFalse(repo.findById(c.getId()).isPresent());
    }

    @Test
    void redoShouldReapplyUndoneCommand() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")), List.of(), null);

        service.createContact(c);
        service.undo();

        assertTrue(service.canRedo());

        service.redo();

        assertTrue(repo.findById(c.getId()).isPresent());
    }

    @Test
    void canUndoShouldReturnFalseWhenStackEmpty() {
        assertFalse(service.canUndo());
    }

    @Test
    void canRedoShouldReturnFalseWhenStackEmpty() {
        assertFalse(service.canRedo());
    }

    @Test
    void deleteContactsBatchShouldRemoveMultiple() {
        Contact c1 = Contact.createNew("A", "One",
                List.of(new PhoneNumber("0671111111")), List.of(), null);
        Contact c2 = Contact.createNew("B", "Two",
                List.of(new PhoneNumber("0672222222")), List.of(), null);
        repo.save(c1);
        repo.save(c2);

        List<UUID> deleted = service.deleteContacts(List.of(c1.getId(), c2.getId()));

        assertEquals(2, deleted.size());
        assertTrue(repo.findAll().isEmpty());
    }

    @Test
    void flushShouldPersistToDisk() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")), List.of(), null);
        service.createContact(c);

        service.flush();

        // Reload to verify persistence
        ContactRepository repo2 = new FileContactRepository(tempDir.resolve("service-test.csv"));
        assertEquals(1, repo2.findAll().size());
    }
}
