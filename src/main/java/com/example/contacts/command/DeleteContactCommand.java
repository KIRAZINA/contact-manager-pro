package com.example.contacts.command;

import com.example.contacts.domain.entity.Contact;
import com.example.contacts.repository.ContactRepository;

import java.util.Objects;

/**
 * Deletes the contact. Keeps a copy for undo.
 */
public final class DeleteContactCommand implements Command {

    private final ContactRepository repo;
    private final Contact contact;
    private boolean executed = false;

    public DeleteContactCommand(ContactRepository repo, Contact contact) {
        this.repo = Objects.requireNonNull(repo);
        this.contact = Objects.requireNonNull(contact);
    }

    @Override
    public void execute() {
        repo.deleteById(contact.getId());
        executed = true;
    }

    @Override
    public void undo() {
        if (executed) {
            repo.save(contact);
            executed = false;
        }
    }

    @Override
    public String description() {
        return "Deleting a contact " + contact.getId();
    }
}
