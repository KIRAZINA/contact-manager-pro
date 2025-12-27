package com.example.contacts.command;

import com.example.contacts.domain.entity.Contact;
import com.example.contacts.domain.enum_.ContactStatus;
import com.example.contacts.repository.ContactRepository;

import java.util.Objects;

public final class ArchiveContactCommand implements Command {

    private final ContactRepository repo;
    private final Contact contact;
    private boolean executed = false;

    public ArchiveContactCommand(ContactRepository repo, Contact contact) {
        this.repo = Objects.requireNonNull(repo);
        this.contact = Objects.requireNonNull(contact);
    }

    @Override
    public void execute() {
        contact.archive();
        repo.save(contact);
        executed = true;
    }

    @Override
    public void undo() {
        if (executed) {
            contact.restore();
            repo.save(contact);
            executed = false;
        }
    }

    @Override
    public String description() {
        return "Archiving a contact " + contact.getId();
    }
}
