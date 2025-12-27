package com.example.contacts.controller;

import com.example.contacts.domain.entity.Contact;
import com.example.contacts.domain.enum_.ContactStatus;
import com.example.contacts.service.ContactService;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller: receives requests from the user (CLI), delegates to ContactService.
 */
public final class ContactController {

    private final ContactService service;

    public ContactController(ContactService service) {
        this.service = service;
    }

    // ---------- CRUD ----------

    public Contact createContact(Contact contact) {
        return service.createContact(contact);
    }

    public Optional<Contact> getContact(UUID id) {
        return service.findContact(id);
    }

    public List<Contact> listContacts() {
        return service.listContacts();
    }

    public Contact updateContact(Contact contact) {
        return service.updateContact(contact);
    }

    public boolean deleteContact(Contact contact) {
        return service.deleteContact(contact);
    }

    // ---------- Archiving / restoration ----------

    public void archiveContact(Contact contact) {
        service.archiveContact(contact);
    }

    public void restoreContact(Contact contact) {
        service.restoreContact(contact);
    }

    // ---------- Undo/Redo ----------

    public void undo() {
        service.undo();
    }

    public void redo() {
        service.redo();
    }

    public boolean canUndo() {
        return service.canUndo();
    }

    public boolean canRedo() {
        return service.canRedo();
    }

    // ---------- Search / filter / sort ----------

    public List<Contact> search(String query) {
        return service.fullTextSearch(query);
    }

    public List<Contact> filterByStatus(ContactStatus status) {
        return service.filterByStatus(status);
    }

    public List<Contact> filterByCreatedDate(LocalDate from, LocalDate to) {
        return service.filterByCreatedDateRange(from, to);
    }

    public List<Contact> sort(Comparator<Contact> comparator) {
        return service.sortContacts(comparator);
    }

    // ---------- Batch ----------

    public List<UUID> deleteContacts(List<UUID> ids) {
        return service.deleteContacts(ids);
    }

    // ---------- Persistence ----------

    public void reload() {
        service.reload();
    }

    public void flush() {
        service.flush();
    }
}
