package com.example.contacts.domain.factory;

import com.example.contacts.domain.entity.Contact;
import com.example.contacts.domain.value.Address;
import com.example.contacts.domain.value.Email;
import com.example.contacts.domain.value.PhoneNumber;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ContactFactory - Factory Pattern.
 */
class ContactFactoryTest {

    @Test
    void createShouldGenerateNewContactWithGeneratedId() {
        List<PhoneNumber> phones = List.of(new PhoneNumber("0671234567"));
        List<Email> emails = List.of(new Email("ivan@test.com"));
        Address address = new Address("Main St", "Kyiv", null, "01001", "Ukraine");

        Contact contact = ContactFactory.create("Ivan", "Petrenko", phones, emails, address);

        assertNotNull(contact.getId());
        assertEquals("Ivan", contact.getFirstName());
        assertEquals("Petrenko", contact.getLastName());
        assertEquals(1, contact.getPhones().size());
        assertEquals(1, contact.getEmails().size());
        assertTrue(contact.getAddress().isPresent());
    }

    @Test
    void createShouldSetStatusToActive() {
        Contact contact = ContactFactory.create("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null);

        assertEquals(com.example.contacts.domain.enum_.ContactStatus.ACTIVE, contact.getStatus());
    }

    @Test
    void createShouldSetTimestamps() {
        Contact contact = ContactFactory.create("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null);

        assertNotNull(contact.getCreatedAt());
        assertNotNull(contact.getUpdatedAt());
    }

    @Test
    void createShouldAllowNullAddress() {
        Contact contact = ContactFactory.create("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null);

        assertTrue(contact.getAddress().isEmpty());
    }

    @Test
    void createShouldNormalizeNames() {
        Contact contact = ContactFactory.create("  Ivan  ", "  Petrenko  ",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null);

        assertEquals("Ivan", contact.getFirstName());
        assertEquals("Petrenko", contact.getLastName());
    }

    @Test
    void createShouldThrowForEmptyFirstName() {
        assertThrows(com.example.contacts.exception.ValidationException.class,
                () -> ContactFactory.create("", "Petrenko",
                        List.of(new PhoneNumber("0671234567")),
                        List.of(new Email("ivan@test.com")), null));
    }

    @Test
    void createShouldThrowForEmptyLastName() {
        assertThrows(com.example.contacts.exception.ValidationException.class,
                () -> ContactFactory.create("Ivan", "",
                        List.of(new PhoneNumber("0671234567")),
                        List.of(new Email("ivan@test.com")), null));
    }

    @Test
    void createShouldThrowForNoPhoneOrEmail() {
        assertThrows(com.example.contacts.exception.ValidationException.class,
                () -> ContactFactory.create("Ivan", "Petrenko",
                        List.of(),
                        List.of(), null));
    }
}
