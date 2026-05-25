package com.example.contacts.repository;

import com.example.contacts.domain.entity.Contact;
import com.example.contacts.domain.enum_.ContactStatus;
import com.example.contacts.domain.value.Address;
import com.example.contacts.domain.value.Email;
import com.example.contacts.domain.value.PhoneNumber;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SqliteContactRepositoryTest {

    @TempDir
    Path tempDir;

    private String dbPath;
    private SqliteContactRepository repo;

    @BeforeEach
    void setup() {
        dbPath = tempDir.resolve("test_contacts.db").toString();
        repo = new SqliteContactRepository(dbPath);
    }

    @AfterEach
    void tearDown() {
        if (repo != null) {
            repo.close();
        }
    }

    @Test
    void saveAndFindByIdShouldWork() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")),
                new Address("Khreshchatyk", "Kyiv", "Kyiv", "01001", "Ukraine"));

        repo.save(c);

        Optional<Contact> loaded = repo.findById(c.getId());
        assertTrue(loaded.isPresent());
        assertEquals("Ivan", loaded.get().getFirstName());
        assertEquals("Petrenko", loaded.get().getLastName());
        assertEquals(1, loaded.get().getPhones().size());
        assertEquals("0671234567", loaded.get().getPhones().get(0).normalized());
        assertEquals(1, loaded.get().getEmails().size());
        assertEquals("ivan@test.com", loaded.get().getEmails().get(0).normalized());
        assertTrue(loaded.get().getAddress().isPresent());
        assertEquals("Khreshchatyk", loaded.get().getAddress().get().street());
    }

    @Test
    void upsertShouldUpdateExistingRecord() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null);

        repo.save(c);

        // Modify the same contact (update last name and add an email)
        Contact updated = new Contact(
                c.getId(),
                "Ivan",
                "Kovalenko",
                c.getPhones(),
                List.of(new Email("ivan@test.com"), new Email("ivan.new@test.com")),
                c.getAddress().orElse(null),
                c.getCreatedAt(),
                c.getUpdatedAt().plusMinutes(5),
                c.getStatus()
        );

        repo.save(updated);

        Optional<Contact> loaded = repo.findById(c.getId());
        assertTrue(loaded.isPresent());
        assertEquals("Kovalenko", loaded.get().getLastName());
        assertEquals(2, loaded.get().getEmails().size());
    }

    @Test
    void reloadAndDurabilityAcrossInstances() {
        Contact c = Contact.createNew("Anna", "Koval",
                List.of(new PhoneNumber("0931112233")),
                List.of(new Email("anna@test.com")), null);

        repo.save(c);
        repo.flush();

        // Create a completely new repository instance pointing to the same file
        try (SqliteContactRepository repo2 = new SqliteContactRepository(dbPath)) {
            Optional<Contact> loaded = repo2.findById(c.getId());
            assertTrue(loaded.isPresent());
            assertEquals("Anna", loaded.get().getFirstName());

            // reload is a no-op but calling it should not cause any exceptions
            assertDoesNotThrow(() -> repo2.reload());
        }
    }

    @Test
    void deleteByIdShouldRemoveRecord() {
        Contact c = Contact.createNew("Petro", "Ivanenko",
                List.of(new PhoneNumber("0509876543")),
                List.of(new Email("petro@test.com")), null);

        repo.save(c);

        assertTrue(repo.deleteById(c.getId()));
        assertFalse(repo.findById(c.getId()).isPresent());
        assertFalse(repo.deleteById(c.getId())); // delete again should return false
    }

    @Test
    void deleteAllByIdShouldRemoveMultipleRecords() {
        Contact c1 = Contact.createNew("A", "One", List.of(), List.of(new Email("a@one.com")), null);
        Contact c2 = Contact.createNew("B", "Two", List.of(), List.of(new Email("b@two.com")), null);
        Contact c3 = Contact.createNew("C", "Three", List.of(), List.of(new Email("c@three.com")), null);

        repo.save(c1);
        repo.save(c2);
        repo.save(c3);

        List<UUID> removed = repo.deleteAllById(List.of(c1.getId(), c3.getId()));
        assertEquals(2, removed.size());
        assertTrue(removed.contains(c1.getId()));
        assertTrue(removed.contains(c3.getId()));

        assertFalse(repo.findById(c1.getId()).isPresent());
        assertTrue(repo.findById(c2.getId()).isPresent());
        assertFalse(repo.findById(c3.getId()).isPresent());
    }

    @Test
    void filterByStatusShouldWork() {
        Contact c1 = Contact.createNew("Active", "User", List.of(), List.of(new Email("active@user.com")), null);
        Contact c2 = Contact.createNew("Archived", "User", List.of(), List.of(new Email("archived@user.com")), null);
        c2.archive();

        repo.save(c1);
        repo.save(c2);

        List<Contact> active = repo.filterByStatus(ContactStatus.ACTIVE);
        assertEquals(1, active.size());
        assertEquals(c1.getId(), active.get(0).getId());

        List<Contact> archived = repo.filterByStatus(ContactStatus.ARCHIVED);
        assertEquals(1, archived.size());
        assertEquals(c2.getId(), archived.get(0).getId());
    }

    @Test
    void fullTextSearchShouldMatchMultipleFields() {
        Contact c1 = Contact.createNew("John", "Doe",
                List.of(new PhoneNumber("+380991234567")),
                List.of(new Email("john.doe@gmail.com")), null);

        Contact c2 = Contact.createNew("Jane", "Smith",
                List.of(new PhoneNumber("+380997654321")),
                List.of(new Email("jane.smith@yahoo.com")), null);

        repo.save(c1);
        repo.save(c2);

        // Search by first name
        List<Contact> search1 = repo.fullTextSearch("john");
        assertEquals(1, search1.size());
        assertEquals(c1.getId(), search1.get(0).getId());

        // Search by last name
        List<Contact> search2 = repo.fullTextSearch("smith");
        assertEquals(1, search2.size());
        assertEquals(c2.getId(), search2.get(0).getId());

        // Search by part of phone
        List<Contact> search3 = repo.fullTextSearch("99123");
        assertEquals(1, search3.size());
        assertEquals(c1.getId(), search3.get(0).getId());

        // Search by domain in email
        List<Contact> search4 = repo.fullTextSearch("yahoo");
        assertEquals(1, search4.size());
        assertEquals(c2.getId(), search4.get(0).getId());

        // Empty search returns empty list
        assertTrue(repo.fullTextSearch("").isEmpty());
        assertTrue(repo.fullTextSearch(null).isEmpty());
    }

    @Test
    void filterByCreatedDateRangeShouldWork() {
        Contact c = Contact.createNew("Date", "Test", List.of(), List.of(new Email("date@test.com")), null);
        repo.save(c);

        LocalDate today = LocalDate.now();
        List<Contact> matches = repo.filterByCreatedDateRange(today, today);
        assertEquals(1, matches.size());

        List<Contact> noMatches = repo.filterByCreatedDateRange(today.plusDays(1), today.plusDays(2));
        assertTrue(noMatches.isEmpty());
    }
}
