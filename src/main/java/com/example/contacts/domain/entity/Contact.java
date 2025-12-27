package com.example.contacts.domain.entity;

import com.example.contacts.domain.enum_.ContactStatus;
import com.example.contacts.domain.value.Address;
import com.example.contacts.domain.value.Email;
import com.example.contacts.domain.value.PhoneNumber;
import com.example.contacts.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Contact unit: responsible for invariants, normalization, uniqueness, and timestamps.
 * First and last names are stored in normalized form (truncated, without extra spaces).
 * Email/phone lists without duplicates after normalization.
 */
public final class Contact {

    private final UUID id;
    private String firstName;
    private String lastName;
    private final List<PhoneNumber> phones;
    private final List<Email> emails;
    private Address address; // optional (can be null if not specified)
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private ContactStatus status;

    public Contact(UUID id,
                   String firstName,
                   String lastName,
                   List<PhoneNumber> phones,
                   List<Email> emails,
                   Address address,
                   LocalDateTime createdAt,
                   LocalDateTime updatedAt,
                   ContactStatus status) {

        this.id = Objects.requireNonNull(id, "id cannot be null");
        setNames(firstName, lastName); // normalizes and validates

        // Initializing time fields before calling addPhone/addEmail
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
        this.updatedAt = (updatedAt != null) ? updatedAt : this.createdAt;
        if (this.updatedAt.isBefore(this.createdAt)) {
            throw new ValidationException("timestamps", "updatedAt cannot be earlier than createdAt");
        }

        this.status = (status != null) ? status : ContactStatus.ACTIVE;

        // Collections
        this.phones = new ArrayList<>();
        this.emails = new ArrayList<>();
        if (phones != null) phones.forEach(this::addPhone);
        if (emails != null) emails.forEach(this::addEmail);

        // Business rule: contact must contain at least a phone number or email address
        if (this.phones.isEmpty() && this.emails.isEmpty()) {
            throw new ValidationException("contact", "The contact must include at least a phone number or email address.");
        }

        // Address may be null (absent)
        if (address != null) {
            this.address = address;
        }
    }


    // --------- Static factory methods for convenience ---------

    public static Contact createNew(String firstName,
                                    String lastName,
                                    List<PhoneNumber> phones,
                                    List<Email> emails,
                                    Address address) {
        LocalDateTime now = LocalDateTime.now();
        return new Contact(UUID.randomUUID(), firstName, lastName, phones, emails,
                address, now, now, ContactStatus.ACTIVE);
    }

    // --------- Getters with mutation protection ---------

    public UUID getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public List<PhoneNumber> getPhones() {
        return Collections.unmodifiableList(phones);
    }

    public List<Email> getEmails() {
        return Collections.unmodifiableList(emails);
    }

    public Optional<Address> getAddress() {
        return Optional.ofNullable(address);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public ContactStatus getStatus() {
        return status;
    }

    // --------- Mutations with invariants ---------

    public void updateNames(String newFirstName, String newLastName) {
        ensureActiveForMutation();
        setNames(newFirstName, newLastName);
        touch();
    }

    public void setAddress(Address newAddress) {
        ensureActiveForMutation();
        this.address = newAddress; // Address — immutable record
        touch();
    }

    public void clearAddress() {
        ensureActiveForMutation();
        this.address = null;
        touch();
    }

    public void addPhone(PhoneNumber phone) {
        Objects.requireNonNull(phone, "phone cannot be null");
        if (!phones.contains(phone)) {
            ensureActiveForMutation();
            phones.add(phone);
            touch();
        }
    }

    public void removePhone(PhoneNumber phone) {
        Objects.requireNonNull(phone, "phone cannot be null");
        int idx = phones.indexOf(phone);
        if (idx >= 0) {
            ensureActiveForMutation();
            phones.remove(idx);
            ensureAtLeastOneContactPoint();
            touch();
        }
    }

    public void addEmail(Email email) {
        Objects.requireNonNull(email, "email cannot be null");
        if (!emails.contains(email)) {
            ensureActiveForMutation();
            emails.add(email);
            touch();
        }
    }

    public void removeEmail(Email email) {
        Objects.requireNonNull(email, "email cannot be null");
        int idx = emails.indexOf(email);
        if (idx >= 0) {
            ensureActiveForMutation();
            emails.remove(idx);
            ensureAtLeastOneContactPoint();
            touch();
        }
    }

    public void archive() {
        if (status == ContactStatus.ARCHIVED) return;
        status = ContactStatus.ARCHIVED;
        touch();
    }

    public void restore() {
        if (status == ContactStatus.ACTIVE) return;
        status = ContactStatus.ACTIVE;
        touch();
    }

    // --------- Auxiliary invariants and normalization ---------

    private void setNames(String firstName, String lastName) {
        String f = normalizeName(firstName);
        String l = normalizeName(lastName);

        if (f == null || f.isBlank()) {
            throw new ValidationException("firstName", "The name cannot be empty");
        }
        if (l == null || l.isBlank()) {
            throw new ValidationException("lastName", "The surname cannot be empty");
        }
        if (f.length() > 100) {
            throw new ValidationException("firstName", "The name is too long");
        }
        if (l.length() > 100) {
            throw new ValidationException("lastName", "The surname is too long");
        }

        this.firstName = f;
        this.lastName = l;
    }

    private static String normalizeName(String s) {
        if (s == null) return null;
        return s.trim().replaceAll("\\s{2,}", " ");
    }

    private void ensureActiveForMutation() {
        if (status == ContactStatus.ARCHIVED) {
            throw new ValidationException("status", "Mutations are prohibited for archived contacts");
        }
    }

    private void ensureAtLeastOneContactPoint() {
        if (phones.isEmpty() && emails.isEmpty()) {
            throw new ValidationException("contact", "The contact must contain at least an email address or telephone number");
        }
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
        if (updatedAt.isBefore(createdAt)) {
            // insurance against systemic time anomalies
            this.updatedAt = createdAt;
        }
    }

    // --------- Derived keys for searching ---------

    public Set<String> searchTokens() {
        Set<String> tokens = new LinkedHashSet<>();
        tokens.addAll(tokenizeName(firstName));
        tokens.addAll(tokenizeName(lastName));
        tokens.addAll(emails.stream().map(Email::normalized).collect(Collectors.toSet()));
        tokens.addAll(phones.stream().map(PhoneNumber::normalized).collect(Collectors.toSet()));
        return Collections.unmodifiableSet(tokens);
    }

    private static List<String> tokenizeName(String n) {
        return Arrays.stream(n.split("\\s+"))
                .map(String::toLowerCase)
                .collect(Collectors.toList());
    }

    // --------- Convenient comparators (for sorting strategies) ---------

    public static Comparator<Contact> comparingByName() {
        return Comparator.comparing(Contact::getLastName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Contact::getFirstName, String.CASE_INSENSITIVE_ORDER);
    }

    public static Comparator<Contact> comparingByCreatedAtAsc() {
        return Comparator.comparing(Contact::getCreatedAt);
    }

    public static Comparator<Contact> comparingByUpdatedAtDesc() {
        return Comparator.comparing(Contact::getUpdatedAt).reversed();
    }
}
