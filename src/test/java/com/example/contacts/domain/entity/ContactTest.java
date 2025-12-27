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

    @Test
    void searchTokensShouldContainNameAndEmail() {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null);

        assertTrue(c.searchTokens().contains("ivan"));
        assertTrue(c.searchTokens().contains("petrenko"));
        assertTrue(c.searchTokens().contains("ivan@test.com"));
    }
}
