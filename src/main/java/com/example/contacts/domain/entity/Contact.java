package com.example.contacts.domain.entity;

import com.example.contacts.domain.enum_.ContactStatus;
import com.example.contacts.domain.value.Address;
import com.example.contacts.domain.value.Email;
import com.example.contacts.domain.value.PhoneNumber;
import com.example.contacts.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Contact aggregate root: responsible for invariants, normalization, and timestamps.
 *
 * Improvements (see Architectural Review):
 *  - Issue 3.3: getPhones() / getEmails() now return List.copyOf() instead of
 *               Collections.unmodifiableList(). The latter only blocks writes through the
 *               wrapper but still reflects mutations in the backing list; List.copyOf()
 *               produces a truly independent snapshot.
 *  - Issue 6.1 / 6.2: searchTokens() and tokenizeName() have been removed.
 *               Search logic now lives entirely in SQL (SqliteContactRepository.SEARCH),
 *               keeping the domain entity free of infrastructure concerns.
 */
public final class Contact {

    private final UUID id;
    private String firstName;
    private String lastName;
    private final List<PhoneNumber> phones;
    private final List<Email> emails;
    private Address address; // optional — use getAddress() which returns Optional<Address>
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

        // Initialize time fields before calling addPhone/addEmail
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
        this.updatedAt = (updatedAt != null) ? updatedAt : this.createdAt;
        if (this.updatedAt.isBefore(this.createdAt)) {
            throw new ValidationException("timestamps", "updatedAt cannot be earlier than createdAt");
        }

        this.status = (status != null) ? status : ContactStatus.ACTIVE;

        // Collections
        this.phones = new ArrayList<>();
        if (phones != null) {
            for (PhoneNumber phone : phones) {
                Objects.requireNonNull(phone, "phone cannot be null");
                if (!this.phones.contains(phone)) {
                    this.phones.add(phone);
                }
            }
        }

        this.emails = new ArrayList<>();
        if (emails != null) {
            for (Email email : emails) {
                Objects.requireNonNull(email, "email cannot be null");
                if (!this.emails.contains(email)) {
                    this.emails.add(email);
                }
            }
        }

        // Business rule: at least one phone or email required
        if (this.phones.isEmpty() && this.emails.isEmpty()) {
            throw new ValidationException("contact",
                    "The contact must include at least a phone number or email address.");
        }

        // Address is optional
        if (address != null) {
            this.address = address;
        }
    }

    // --------- Static factory methods ---------

    public static Contact createNew(String firstName,
                                    String lastName,
                                    List<PhoneNumber> phones,
                                    List<Email> emails,
                                    Address address) {
        LocalDateTime now = LocalDateTime.now();
        return new Contact(UUID.randomUUID(), firstName, lastName, phones, emails,
                address, now, now, ContactStatus.ACTIVE);
    }

    // --------- Getters with true immutability (Issue 3.3) ---------

    public UUID getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    /**
     * Returns a true immutable snapshot of the phone list (List.copyOf).
     * Unlike Collections.unmodifiableList, changes to the internal list
     * are NOT reflected in the returned list.
     */
    public List<PhoneNumber> getPhones() {
        return List.copyOf(phones);
    }

    /**
     * Returns a true immutable snapshot of the email list (List.copyOf).
     */
    public List<Email> getEmails() {
        return List.copyOf(emails);
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
        this.address = newAddress; // Address is an immutable record
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
        this.lastName  = l;
    }

    private static String normalizeName(String s) {
        if (s == null) return null;
        return s.trim().replaceAll("\\s{2,}", " ");
    }

    private void ensureActiveForMutation() {
        if (status == ContactStatus.ARCHIVED) {
            throw new ValidationException("status",
                    "Mutations are prohibited for archived contacts");
        }
    }

    private void ensureAtLeastOneContactPoint() {
        if (phones.isEmpty() && emails.isEmpty()) {
            throw new ValidationException("contact",
                    "The contact must contain at least an email address or telephone number");
        }
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
        if (updatedAt.isBefore(createdAt)) {
            // Insurance against system clock anomalies
            this.updatedAt = createdAt;
        }
    }

    // --------- Convenient comparators ---------

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
