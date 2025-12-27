package com.example.contacts.command;

import com.example.contacts.domain.entity.Contact;
import com.example.contacts.repository.ContactRepository;

import java.util.Objects;

/**
 * Creates a new contact in the repository.
 */
public final class CreateContactCommand implements Command {

    private final ContactRepository repo;
    private final Contact contact;
    private boolean executed = false;

    public CreateContactCommand(ContactRepository repo, Contact contact) {
        this.repo = Objects.requireNonNull(repo);
        this.contact = Objects.requireNonNull(contact);
    }

    @Override
    public void execute() {
        repo.save(contact);
        executed = true;
    }

    @Override
    public void undo() {
        if (executed) {
            repo.deleteById(contact.getId());
            executed = false;
        }
    }

    @Override
    public String description() {
        return "Creating a contact " + contact.getFirstName() + " " + contact.getLastName();
    }
}
