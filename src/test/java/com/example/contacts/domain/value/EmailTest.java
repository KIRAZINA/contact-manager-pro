package com.example.contacts.domain.value;

import com.example.contacts.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailTest {

    @Test
    void validEmailShouldBeAccepted() {
        Email e = new Email("User@Test.COM");
        assertEquals("user@test.com", e.normalized());
    }

    @Test
    void invalidEmailShouldThrowException() {
        assertThrows(ValidationException.class, () -> new Email("bad-email"));
    }

    @Test
    void equalityShouldIgnoreCase() {
        Email e1 = new Email("Test@Mail.com");
        Email e2 = new Email("test@mail.com");
        assertEquals(e1, e2);
    }
}
