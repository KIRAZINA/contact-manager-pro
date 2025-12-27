package com.example.contacts.repository;

import com.example.contacts.domain.entity.Contact;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

public interface ContactRepository {

    Optional<Contact> findById(UUID id);

    List<Contact> findAll();

    // Search by predicate (used by service/strategies)
    List<Contact> findWhere(Predicate<Contact> predicate);

    // Full-text search (name, email, phone number)
    List<Contact> fullTextSearch(String query);

    // Filters
    List<Contact> filterByStatus(com.example.contacts.domain.enum_.ContactStatus status);

    List<Contact> filterByCreatedDateRange(LocalDate fromInclusive, LocalDate toInclusive);

    // CRUD
    Contact save(Contact contact); // create or update by id

    boolean deleteById(UUID id);

    // Batch operations
    List<UUID> deleteAllById(List<UUID> ids);

    // Synchronization with disk (for file implementation)
    void reload();

    void flush();
}
