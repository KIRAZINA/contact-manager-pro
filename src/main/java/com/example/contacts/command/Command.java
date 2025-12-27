package com.example.contacts.command;

public sealed interface Command
        permits CreateContactCommand, UpdateContactCommand, DeleteContactCommand, ArchiveContactCommand, RestoreContactCommand {

    void execute();

    void undo();

    String description();
}
