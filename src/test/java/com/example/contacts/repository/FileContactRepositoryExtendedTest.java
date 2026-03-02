package com.example.contacts.repository;

import com.example.contacts.domain.entity.Contact;
import com.example.contacts.domain.enum_.ContactStatus;
import com.example.contacts.domain.value.Email;
import com.example.contacts.domain.value.PhoneNumber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Extended integration tests for FileContactRepository.
 * Covers: full-text search, date filtering, findWhere predicate.
 */
class FileContactRepositoryExtendedTest {

    @TempDir
    Path tempDir;
    
    private Path testFile;
    private FileContactRepository repo;

    @BeforeEach
    void setup() throws Exception {
        testFile = tempDir.resolve("extended-test.csv");
        if (Files.exists(testFile)) {
            Files.delete(testFile);
        }
        repo = new FileContactRepository(testFile);
    }

    // ========== Full-Text Search ==========

    @Test
    void fullTextSearchShouldFindByFirstName() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null);
        repo.save(c);

        List<Contact> results = repo.fullTextSearch("Ivan");

        assertEquals(1, results.size());
        assertEquals("Ivan", results.get(0).getFirstName());
    }

    @Test
    void fullTextSearchShouldFindByLastName() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null);
        repo.save(c);

        List<Contact> results = repo.fullTextSearch("Petrenko");

        assertEquals(1, results.size());
    }

    @Test
    void fullTextSearchShouldFindByEmail() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null);
        repo.save(c);

        List<Contact> results = repo.fullTextSearch("ivan@test");

        assertEquals(1, results.size());
    }

    @Test
    void fullTextSearchShouldFindByPhone() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null);
        repo.save(c);

        List<Contact> results = repo.fullTextSearch("067123");

        assertEquals(1, results.size());
    }

    @Test
    void fullTextSearchShouldBeCaseInsensitive() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("IVAN@TEST.COM")), null);
        repo.save(c);

        List<Contact> results = repo.fullTextSearch("IVAN");

        assertEquals(1, results.size());
    }

    @Test
    void fullTextSearchShouldReturnEmptyForNoMatch() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null);
        repo.save(c);

        List<Contact> results = repo.fullTextSearch("NoExistingContact");

        assertTrue(results.isEmpty());
    }

    @Test
    void fullTextSearchShouldHandleEmptyQuery() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null);
        repo.save(c);

        List<Contact> results = repo.fullTextSearch("");

        assertTrue(results.isEmpty());
    }

    @Test
    void fullTextSearchShouldMatchPartialTokens() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null);
        repo.save(c);

        // "pet" should match "Petrenko"
        List<Contact> results = repo.fullTextSearch("pet");

        assertEquals(1, results.size());
    }

    // ========== Filter by Date Range ==========

    @Test
    void filterByCreatedDateRangeShouldReturnMatchingContacts() {
        Contact c1 = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(), null);
        Contact c2 = Contact.createNew("Petro", "Ivanenko",
                List.of(new PhoneNumber("0509876543")),
                List.of(), null);
        repo.save(c1);
        repo.save(c2);

        LocalDate today = LocalDate.now();
        List<Contact> results = repo.filterByCreatedDateRange(today, today);

        assertTrue(results.size() >= 1);
    }

    @Test
    void filterByCreatedDateRangeShouldReturnEmptyWhenNoMatch() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(), null);
        repo.save(c);

        LocalDate future = LocalDate.now().plusDays(100);
        List<Contact> results = repo.filterByCreatedDateRange(future, future.plusDays(1));

        assertTrue(results.isEmpty());
    }

    @Test
    void filterByCreatedDateRangeShouldHandleNullFromDate() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(), null);
        repo.save(c);

        List<Contact> results = repo.filterByCreatedDateRange(null, LocalDate.now());

        assertTrue(results.size() >= 1);
    }

    @Test
    void filterByCreatedDateRangeShouldHandleNullToDate() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(), null);
        repo.save(c);

        List<Contact> results = repo.filterByCreatedDateRange(LocalDate.now(), null);

        assertTrue(results.size() >= 1);
    }

    // ========== FindWhere Predicate ==========

    @Test
    void findWhereShouldFilterByPredicate() {
        Contact c1 = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null);
        Contact c2 = Contact.createNew("Petro", "Ivanenko",
                List.of(new PhoneNumber("0509876543")),
                List.of(new Email("petro@test.com")), null);
        repo.save(c1);
        repo.save(c2);

        List<Contact> results = repo.findWhere(c -> c.getFirstName().equals("Ivan"));

        assertEquals(1, results.size());
        assertEquals("Petrenko", results.get(0).getLastName());
    }

    @Test
    void findWhereShouldReturnAllWhenPredicateMatchesAll() {
        Contact c1 = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(), null);
        Contact c2 = Contact.createNew("Petro", "Ivanenko",
                List.of(new PhoneNumber("0509876543")),
                List.of(), null);
        repo.save(c1);
        repo.save(c2);

        List<Contact> results = repo.findWhere(c -> true);

        assertEquals(2, results.size());
    }

    @Test
    void findWhereShouldReturnEmptyWhenNoMatch() {
        Contact contact = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(), null);
        repo.save(contact);

        List<Contact> results = repo.findWhere(c -> c.getFirstName().equals("NonExistent"));

        assertTrue(results.isEmpty());
    }

    // ========== Edge Cases ==========

    @Test
    void findAllShouldReturnUnmodifiableList() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(), null);
        repo.save(c);

        List<Contact> all = repo.findAll();

        assertThrows(UnsupportedOperationException.class, () -> all.add(c));
    }

    @Test
    void saveShouldHandleDuplicateId() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(), null);
        repo.save(c);

        // Try to save with same ID (should skip)
        Contact result = repo.save(c);

        assertEquals(c.getId(), result.getId());
    }

    @Test
    void deleteAllByIdShouldReturnOnlyDeletedIds() {
        Contact c1 = Contact.createNew("A", "One",
                List.of(new PhoneNumber("0671111111")), List.of(), null);
        Contact c2 = Contact.createNew("B", "Two",
                List.of(new PhoneNumber("0672222222")), List.of(), null);
        Contact c3 = Contact.createNew("C", "Three",
                List.of(new PhoneNumber("0673333333")), List.of(), null);
        repo.save(c1);
        repo.save(c2);
        repo.save(c3);

        // Delete first and third (second doesn't exist)
        List<UUID> deleted = repo.deleteAllById(List.of(c1.getId(), c3.getId(), UUID.randomUUID()));

        assertEquals(2, deleted.size());
    }

    @Test
    void reloadShouldClearAndReloadFromFile() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(), null);
        repo.save(c);
        repo.flush();

        repo.reload();

        assertEquals(1, repo.findAll().size());
    }
}
