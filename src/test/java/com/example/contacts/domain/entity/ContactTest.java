package com.example.contacts.domain.entity;

import com.example.contacts.domain.value.Email;
import com.example.contacts.domain.value.PhoneNumber;
import com.example.contacts.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ContactTest {

    @Test
    void contactMustHaveAtLeastPhoneOrEmail() {
        assertThrows(ValidationException.class,
                () -> Contact.createNew("Ivan", "Petrenko", List.of(), List.of(), null));
    }

    @Test
    void archiveAndRestoreShouldChangeStatus() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")), List.of(), null);

        c.archive();
        assertEquals(com.example.contacts.domain.enum_.ContactStatus.ARCHIVED, c.getStatus());

        c.restore();
        assertEquals(com.example.contacts.domain.enum_.ContactStatus.ACTIVE, c.getStatus());
    }

    @Test
    void removingLastPhoneShouldThrowException() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")), List.of(), null);

        assertThrows(ValidationException.class, () -> c.removePhone(new PhoneNumber("0671234567")));
    }

    /**
     * Verifies Issue 3.3 fix: getPhones/getEmails return true immutable snapshots
     * (List.copyOf) — changes to the returned list must not affect the contact.
     */
    @Test
    void getPhonesShouldReturnImmutableSnapshot() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")), List.of(new Email("ivan@test.com")), null);

        List<PhoneNumber> snapshot = c.getPhones();

        // The returned list itself must be unmodifiable
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.add(new PhoneNumber("0509999999")));

        // The contact's phone count must be unchanged
        assertEquals(1, c.getPhones().size());
    }

    @Test
    void getEmailsShouldReturnImmutableSnapshot() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")), List.of(new Email("ivan@test.com")), null);

        List<Email> snapshot = c.getEmails();

        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.add(new Email("other@test.com")));

        assertEquals(1, c.getEmails().size());
    }

    @Test
    void namesAreTrimmedAndNormalized() {
        Contact c = Contact.createNew("  Ivan  ", "  Petrenko  ",
                List.of(new PhoneNumber("0671234567")), List.of(), null);
        assertEquals("Ivan", c.getFirstName());
        assertEquals("Petrenko", c.getLastName());
    }
}
