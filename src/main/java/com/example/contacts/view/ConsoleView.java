package com.example.contacts.view;

import com.example.contacts.controller.ContactController;
import com.example.contacts.domain.entity.Contact;
import com.example.contacts.domain.factory.ContactFactory;
import com.example.contacts.domain.value.Email;
import com.example.contacts.domain.value.PhoneNumber;
import com.example.contacts.domain.value.Address;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Console interface for user interaction.
 */
public final class ConsoleView {

    private final Scanner scanner = new Scanner(System.in);
    private final ContactController controller;

    public ConsoleView(ContactController controller) {
        this.controller = controller;
    }

    public void start() {
        System.out.println("=== Contact Manager Pro ===");
        boolean running = true;
        while (running) {
            showMenu();
            String cmd = scanner.nextLine().trim().toLowerCase();
            switch (cmd) {
                case "1" -> addContact();
                case "2" -> listContacts();
                case "3" -> searchContacts();
                case "4" -> deleteContact();
                case "5" -> archiveContact();
                case "6" -> restoreContact();
                case "7" -> undo();
                case "8" -> redo();
                case "9" -> running = false;
                default -> System.out.println("Unknown command");
            }
        }
        System.out.println("Exit...");
    }

    private void showMenu() {
        System.out.println("\nMenu:");
        System.out.println("1. Add contact");
        System.out.println("2. Contact list");
        System.out.println("3. Search");
        System.out.println("4. Delete contact");
        System.out.println("5. Archive contact");
        System.out.println("6. Restore contact");
        System.out.println("7. Undo");
        System.out.println("8. Redo");
        System.out.println("9. Exit");
        System.out.print("Select an action: ");
    }

    private void addContact() {
        System.out.print("Name: ");
        String firstName = scanner.nextLine();
        System.out.print("Last name: ");
        String lastName = scanner.nextLine();

        System.out.print("Telephones (separated by commas): ");
        String phonesRaw = scanner.nextLine();
        List<PhoneNumber> phones = Arrays.stream(phonesRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(PhoneNumber::new)
                .collect(Collectors.toList());

        System.out.print("Електронні листи (через кому): ");
        String emailsRaw = scanner.nextLine();
        List<Email> emails = Arrays.stream(emailsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Email::new)
                .collect(Collectors.toList());

        System.out.print("Address (street, city, region, postal code, country): ");
        String addrRaw = scanner.nextLine();
        Address address = null;
        if (!addrRaw.isBlank()) {
            String[] parts = addrRaw.split(",");
            address = new Address(
                    parts.length > 0 ? parts[0].trim() : null,
                    parts.length > 1 ? parts[1].trim() : null,
                    parts.length > 2 ? parts[2].trim() : null,
                    parts.length > 3 ? parts[3].trim() : null,
                    parts.length > 4 ? parts[4].trim() : null
            );
        }

        Contact c = ContactFactory.create(firstName, lastName, phones, emails, address);
        controller.createContact(c);
        System.out.println("Contact created: " + c.getId());
    }

    private void listContacts() {
        List<Contact> contacts = controller.listContacts();
        if (contacts.isEmpty()) {
            System.out.println("No contacts");
            return;
        }
        contacts.forEach(c -> {
            System.out.println(c.getId() + " | " + c.getFirstName() + " " + c.getLastName()
                    + " | " + c.getStatus()
                    + " | phones=" + c.getPhones()
                    + " | emails=" + c.getEmails());
        });
    }

    private void searchContacts() {
        System.out.print("Search query: ");
        String q = scanner.nextLine();
        List<Contact> results = controller.search(q);
        if (results.isEmpty()) {
            System.out.println("Nothing found");
            return;
        }
        results.forEach(c -> System.out.println(c.getId() + " | " + c.getFirstName() + " " + c.getLastName()));
    }

    private void deleteContact() {
        System.out.print("Contact ID to delete: ");
        String idRaw = scanner.nextLine();
        try {
            UUID id = UUID.fromString(idRaw);
            Optional<Contact> c = controller.getContact(id);
            if (c.isPresent()) {
                controller.deleteContact(c.get());
                System.out.println("Contact deleted");
            } else {
                System.out.println("Contact not found");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Incorrect UUID");
        }
    }

    private void archiveContact() {
        System.out.print("Contact ID for archiving: ");
        String idRaw = scanner.nextLine();
        try {
            UUID id = UUID.fromString(idRaw);
            Optional<Contact> c = controller.getContact(id);
            if (c.isPresent()) {
                controller.archiveContact(c.get());
                System.out.println("Contact archived");
            } else {
                System.out.println("Contact not found");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Incorrect UUID");
        }
    }

    private void restoreContact() {
        System.out.print("Contact ID for recovery: ");
        String idRaw = scanner.nextLine();
        try {
            UUID id = UUID.fromString(idRaw);
            Optional<Contact> c = controller.getContact(id);
            if (c.isPresent()) {
                controller.restoreContact(c.get());
                System.out.println("Contact restored");
            } else {
                System.out.println("Contact not found");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Incorrect UUID");
        }
    }

    private void undo() {
        if (controller.canUndo()) {
            controller.undo();
            System.out.println("Undo completed");
        } else {
            System.out.println("No commands for undo");
        }
    }

    private void redo() {
        if (controller.canRedo()) {
            controller.redo();
            System.out.println("Redo completed");
        } else {
            System.out.println("No commands for redo");
        }
    }
}
