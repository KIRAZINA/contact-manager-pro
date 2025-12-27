package com.example.contacts.command;

import com.example.contacts.domain.entity.Contact;
import com.example.contacts.repository.ContactRepository;

import java.util.Objects;

/**
 * Updates contact information (e.g., name/address).
 * Saves the previous state for undo.
 */
public final class UpdateContactCommand implements Command {

    private final ContactRepository repo;
    private final Contact newState;
    private Contact oldState;
    private boolean executed = false;

    public UpdateContactCommand(ContactRepository repo, Contact newState) {
        this.repo = Objects.requireNonNull(repo);
        this.newState = Objects.requireNonNull(newState);
    }

    @Override
    public void execute() {
        oldState = repo.findById(newState.getId()).orElseThrow();
        repo.save(newState);
        executed = true;
    }

    @Override
    public void undo() {
        if (executed && oldState != null) {
            repo.save(oldState);
            executed = false;
        }
    }

    @Override
    public String description() {
        return "Update contact " + newState.getId();
    }
}
