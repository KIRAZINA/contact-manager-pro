package com.example.contacts.repository;

import com.example.contacts.domain.entity.Contact;
import com.example.contacts.domain.enum_.ContactStatus;
import com.example.contacts.domain.value.Address;
import com.example.contacts.domain.value.Email;
import com.example.contacts.domain.value.PhoneNumber;
import com.example.contacts.exception.ValidationException;
import com.example.contacts.util.CsvUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Stores all contacts in a single CSV file.
 * String format:
 * id,firstName,lastName,phones,emails,address,createdAt,updatedAt,status
 *
 * - phones: phone1|phone2|...
 * - emails: email1|email2|...
 * - address: street|city|region|postalCode|country (empty fields are allowed if the address is missing)
 * - date in ISO format: LocalDateTime.toString()
 *
 * Flush atomicity: write to temporary file + Files.move(..., ATOMIC_MOVE/REPLACE_EXISTING).
 */
public final class FileContactRepository implements ContactRepository {

    private final Path file;
    private final Map<UUID, Contact> byId = new LinkedHashMap<>();
    private volatile boolean loaded = false;

    public FileContactRepository(Path file) {
        this.file = Objects.requireNonNull(file, "file path must not be null");
        ensureFileExists();
        reload();
    }

    private void ensureFileExists() {
        try {
            Path parent = file.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            if (!Files.exists(file)) {
                try (BufferedWriter writer = Files.newBufferedWriter(
                        file,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE
                )) {
                    writer.write(header());
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to prepare repository file: " + file, e);
        }
    }

    private static String header() {
        return "id,firstName,lastName,phones,emails,address,createdAt,updatedAt,status";
    }

    @Override
    public synchronized Optional<Contact> findById(UUID id) {
        ensureLoaded();
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public synchronized List<Contact> findAll() {
        ensureLoaded();
        return Collections.unmodifiableList(new ArrayList<>(byId.values()));
    }

    @Override
    public synchronized List<Contact> findWhere(Predicate<Contact> predicate) {
        ensureLoaded();
        return byId.values().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    @Override
    public synchronized List<Contact> fullTextSearch(String query) {
        ensureLoaded();
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        if (normalizedQuery.isEmpty()) {
            return List.of();
        }
        return byId.values().stream()
                .filter(contact -> contact.searchTokens().stream()
                        .anyMatch(token -> token.toLowerCase().contains(normalizedQuery)))
                .collect(Collectors.toList());
    }

    @Override
    public synchronized List<Contact> filterByStatus(ContactStatus status) {
        ensureLoaded();
        return byId.values().stream()
                .filter(contact -> contact.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public synchronized List<Contact> filterByCreatedDateRange(LocalDate fromInclusive, LocalDate toInclusive) {
        ensureLoaded();
        return byId.values().stream()
                .filter(contact -> {
                    LocalDate createdDate = contact.getCreatedAt().toLocalDate();
                    boolean isAfterOrEqual = fromInclusive == null || !createdDate.isBefore(fromInclusive);
                    boolean isBeforeOrEqual = toInclusive == null || !createdDate.isAfter(toInclusive);
                    return isAfterOrEqual && isBeforeOrEqual;
                })
                .collect(Collectors.toList());
    }

    @Override
    public synchronized Contact save(Contact contact) {
        ensureLoaded();
        byId.put(contact.getId(), contact);
        return contact;
    }

    @Override
    public synchronized boolean deleteById(UUID id) {
        ensureLoaded();
        return byId.remove(id) != null;
    }

    @Override
    public synchronized List<UUID> deleteAllById(List<UUID> ids) {
        ensureLoaded();
        List<UUID> removed = new ArrayList<>();
        for (UUID id : ids) {
            if (byId.remove(id) != null) {
                removed.add(id);
            }
        }
        return removed;
    }

    @Override
    public synchronized void reload() {
        Map<UUID, Contact> newMap = new LinkedHashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) {
                    first = false;
                    if (line.trim().equalsIgnoreCase(header())) {
                        continue;
                    }
                }
                if (line.trim().isEmpty()) {
                    continue;
                }
                Contact contact = parseContact(line);
                newMap.put(contact.getId(), contact);
            }
        } catch (IOException e) {
            System.err.println("Error reading repository file: " + file + " | " + e.getMessage());
            throw new RuntimeException("Repository read error: " + file, e);
        }
        byId.clear();
        byId.putAll(newMap);
        loaded = true;
    }

    @Override
    public synchronized void flush() {
        ensureLoaded();
        Path tmp = file.resolveSibling(file.getFileName().toString() + ".tmp");
        try (BufferedWriter writer = Files.newBufferedWriter(
                tmp,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        )) {
            writer.write(header());
            writer.newLine();
            for (Contact contact : byId.values()) {
                writer.write(serializeContact(contact));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Temporary file write error: " + tmp, e);
        }

        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                throw new RuntimeException("Repository file replacement error: " + file, ex);
            }
        } catch (IOException e) {
            throw new RuntimeException("Repository file replacement error: " + file, e);
        }
    }

    private String serializeContact(Contact contact) {
        List<String> cells = new ArrayList<>(9);
        cells.add(contact.getId().toString());
        cells.add(contact.getFirstName());
        cells.add(contact.getLastName());
        cells.add(serializePhones(contact.getPhones()));
        cells.add(serializeEmails(contact.getEmails()));
        cells.add(serializeAddress(contact.getAddress().orElse(null)));
        cells.add(contact.getCreatedAt().toString());
        cells.add(contact.getUpdatedAt().toString());
        cells.add(contact.getStatus().name());
        return CsvUtil.joinEscaped(cells);
    }

    private static String serializePhones(List<PhoneNumber> phones) {
        if (phones == null || phones.isEmpty()) {
            return "";
        }
        return phones.stream()
                .map(PhoneNumber::normalized)
                .collect(Collectors.joining("|"));
    }

    private static String serializeEmails(List<Email> emails) {
        if (emails == null || emails.isEmpty()) {
            return "";
        }
        return emails.stream()
                .map(Email::normalized)
                .collect(Collectors.joining("|"));
    }

    private static String serializeAddress(Address address) {
        if (address == null) {
            return "";
        }
        List<String> parts = List.of(
                nullSafe(address.street()),
                nullSafe(address.city()),
                nullSafe(address.region()),
                nullSafe(address.postalCode()),
                nullSafe(address.country())
        );
        return String.join("|", parts);
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private Contact parseContact(String line) {
        List<String> cells = CsvUtil.parseLine(line);
        if (cells.size() < 9) {
            throw new RuntimeException("Incorrect CSV string: 9 fields expected, received " + cells.size());
        }

        UUID id = UUID.fromString(cells.get(0));
        String firstName = cells.get(1);
        String lastName = cells.get(2);
        List<PhoneNumber> phones = parsePhones(cells.get(3));
        List<Email> emails = parseEmails(cells.get(4));
        Address address = parseAddress(cells.get(5));
        LocalDateTime createdAt = LocalDateTime.parse(cells.get(6));
        LocalDateTime updatedAt = LocalDateTime.parse(cells.get(7));
        ContactStatus status = ContactStatus.valueOf(cells.get(8));

        return new Contact(id, firstName, lastName, phones, emails, address, createdAt, updatedAt, status);
    }

    private List<PhoneNumber> parsePhones(String cell) {
        if (cell == null || cell.isEmpty()) {
            return new ArrayList<>();
        }

        String[] parts = cell.split("\\|");
        List<PhoneNumber> out = new ArrayList<>(parts.length);
        for (String part : parts) {
            if (!part.isEmpty()) {
                out.add(new PhoneNumber(part));
            }
        }
        return out.stream().distinct().collect(Collectors.toCollection(ArrayList::new));
    }

    private List<Email> parseEmails(String cell) {
        if (cell == null || cell.isEmpty()) {
            return new ArrayList<>();
        }

        String[] parts = cell.split("\\|");
        List<Email> out = new ArrayList<>(parts.length);
        for (String part : parts) {
            if (!part.isEmpty()) {
                out.add(new Email(part));
            }
        }
        return out.stream().distinct().collect(Collectors.toCollection(ArrayList::new));
    }

    private Address parseAddress(String cell) {
        if (cell == null || cell.isEmpty()) {
            return null;
        }

        String[] parts = cell.split("\\|", -1);
        String street = parts.length > 0 && !parts[0].isEmpty() ? parts[0] : null;
        String city = parts.length > 1 && !parts[1].isEmpty() ? parts[1] : null;
        String region = parts.length > 2 && !parts[2].isEmpty() ? parts[2] : null;
        String postal = parts.length > 3 && !parts[3].isEmpty() ? parts[3] : null;
        String country = parts.length > 4 && !parts[4].isEmpty() ? parts[4] : null;

        try {
            return new Address(street, city, region, postal, country);
        } catch (ValidationException e) {
            throw e;
        }
    }

    private void ensureLoaded() {
        if (!loaded) {
            reload();
        }
    }
}
