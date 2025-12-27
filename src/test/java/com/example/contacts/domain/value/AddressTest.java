package com.example.contacts.domain.value;

import com.example.contacts.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AddressTest {

    @Test
    void validAddressShouldBeAccepted() {
        Address a = new Address("Main St", "Kyiv", null, "01001", "Ukraine");
        assertEquals("Main St, Kyiv, 01001, Ukraine", a.toString());
    }

    @Test
    void emptyAddressShouldThrowException() {
        assertThrows(ValidationException.class, () -> new Address(null, null, null, null, null));
    }

    @Test
    void normalizationShouldTrimSpaces() {
        Address a = new Address("  Street   ", "  City  ", null, null, null);
        assertEquals("Street, City", a.toString());
    }
}
