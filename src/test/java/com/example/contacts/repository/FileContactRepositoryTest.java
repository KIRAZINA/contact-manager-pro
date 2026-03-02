package com.example.contacts.repository;

import com.example.contacts.domain.entity.Contact;
import com.example.contacts.domain.enum_.ContactStatus;
import com.example.contacts.domain.value.Email;
import com.example.contacts.domain.value.PhoneNumber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FileContactRepositoryTest {

    private Path testFile;
    private FileContactRepository repo;

    @BeforeEach
    void setup() throws Exception {
        testFile = Path.of("target", "repo-test.csv");
        if (Files.exists(testFile)) {
            Files.delete(testFile);
        }
        repo = new FileContactRepository(testFile);
    }

    @Test
    void saveAndFindByIdShouldWork() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null);

        repo.save(c);
        repo.flush();

        Optional<Contact> loaded = repo.findById(c.getId());
        assertTrue(loaded.isPresent());
        assertEquals("Ivan", loaded.get().getFirstName());
    }

    @Test
    void reloadShouldRestoreContactsFromFile() {
        Contact c1 = Contact.createNew("Anna", "Koval",
                List.of(new PhoneNumber("0931112233")),
                List.of(new Email("anna@test.com")), null);

        Contact c2 = Contact.createNew("Oleh", "Bondar",
                List.of(new PhoneNumber("0934445566")),
                List.of(new Email("oleh@test.com")), null);

        repo.save(c1);
        repo.save(c2);
        repo.flush();

        // Create a new repository instance to test reload
        FileContactRepository repo2 = new FileContactRepository(testFile);
        List<Contact> contacts = repo2.findAll();

        assertEquals(2, contacts.size());
        assertTrue(contacts.stream().anyMatch(c -> c.getFirstName().equals("Anna")));
        assertTrue(contacts.stream().anyMatch(c -> c.getFirstName().equals("Oleh")));
    }

    @Test
    void deleteByIdShouldRemoveContact() {
        Contact c = Contact.createNew("Petro", "Ivanenko",
                List.of(new PhoneNumber("0509876543")),
                List.of(new Email("petro@test.com")), null);

        repo.save(c);
        repo.flush();

        boolean deleted = repo.deleteById(c.getId());
        assertTrue(deleted);

        repo.flush();
        assertFalse(repo.findById(c.getId()).isPresent());
    }

    @Test
    void filterByStatusShouldReturnOnlyActiveOrArchived() {
        Contact c = Contact.createNew("Test", "User",
                List.of(new PhoneNumber("0670000000")),
                List.of(new Email("test@user.com")), null);

        repo.save(c);
        repo.flush();

        // Default status is ACTIVE
        List<Contact> active = repo.filterByStatus(ContactStatus.ACTIVE);
        assertEquals(1, active.size());

        // Archive the contact
        c.archive();
        repo.save(c);
        repo.flush();

        List<Contact> archived = repo.filterByStatus(ContactStatus.ARCHIVED);
        assertEquals(1, archived.size());
    }

    @Test
    void deleteAllByIdShouldRemoveMultipleContacts() {
        Contact c1 = Contact.createNew("A", "One",
                List.of(new PhoneNumber("0671111111")),
                List.of(new Email("a@one.com")), null);

        Contact c2 = Contact.createNew("B", "Two",
                List.of(new PhoneNumber("0672222222")),
                List.of(new Email("b@two.com")), null);

        repo.save(c1);
        repo.save(c2);
        repo.flush();

        List<UUID> removed = repo.deleteAllById(List.of(c1.getId(), c2.getId()));
        assertEquals(2, removed.size());
        repo.flush();

        assertTrue(repo.findAll().isEmpty());
    }
}
