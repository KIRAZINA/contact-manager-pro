package com.example.contacts.service;

import com.example.contacts.command.*;
import com.example.contacts.domain.entity.Contact;
import com.example.contacts.domain.enum_.ContactStatus;
import com.example.contacts.repository.ContactRepository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Application Service: facade for the controller.
 * Encapsulates work with the repository, commands, and CommandManager.
 */
public final class ContactService {

    private final ContactRepository repo;
    private final CommandManager commandManager;

    public ContactService(ContactRepository repo, CommandManager commandManager) {
        this.repo = repo;
        this.commandManager = commandManager;
    }

    // ---------- CRUD ----------

    public Contact createContact(Contact contact) {
        CreateContactCommand cmd = new CreateContactCommand(repo, contact);
        commandManager.executeCommand(cmd);
        return contact;
    }

    public Optional<Contact> findContact(UUID id) {
        return repo.findById(id);
    }

    public List<Contact> listContacts() {
        return repo.findAll();
    }

    public Contact updateContact(Contact newState) {
        UpdateContactCommand cmd = new UpdateContactCommand(repo, newState);
        commandManager.executeCommand(cmd);
        return newState;
    }

    public boolean deleteContact(Contact contact) {
        DeleteContactCommand cmd = new DeleteContactCommand(repo, contact);
        commandManager.executeCommand(cmd);
        return true;
    }

    // ---------- Archiving / restoration ----------

    public void archiveContact(Contact contact) {
        ArchiveContactCommand cmd = new ArchiveContactCommand(repo, contact);
        commandManager.executeCommand(cmd);
    }

    public void restoreContact(Contact contact) {
        RestoreContactCommand cmd = new RestoreContactCommand(repo, contact);
        commandManager.executeCommand(cmd);
    }

    // ---------- Undo/Redo ----------

    public boolean canUndo() {
        return commandManager.canUndo();
    }

    public boolean canRedo() {
        return commandManager.canRedo();
    }

    public void undo() {
        commandManager.undo();
    }

    public void redo() {
        commandManager.redo();
    }

    // ---------- Search / filter / sort ----------

    public List<Contact> fullTextSearch(String query) {
        return repo.fullTextSearch(query);
    }

    public List<Contact> filterByStatus(ContactStatus status) {
        return repo.filterByStatus(status);
    }

    public List<Contact> filterByCreatedDateRange(LocalDate from, LocalDate to) {
        return repo.filterByCreatedDateRange(from, to);
    }

    public List<Contact> sortContacts(Comparator<Contact> comparator) {
        return repo.findAll().stream().sorted(comparator).toList();
    }

    public List<Contact> filterContacts(Predicate<Contact> predicate) {
        return repo.findWhere(predicate);
    }

    // ---------- Batch operations ----------

    public List<UUID> deleteContacts(List<UUID> ids) {
        return repo.deleteAllById(ids);
    }

    // ---------- Persistence ----------

    public void reload() {
        repo.reload();
    }

    public void flush() {
        repo.flush();
    }

    public int getUndoStackSize() {
        return commandManager.getUndoStackSize();
    }

    public int getRedoStackSize() {
        return commandManager.getRedoStackSize();
    }

}
