package com.example.contacts.domain.value;

import com.example.contacts.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhoneNumberTest {

    @Test
    void validPhoneShouldBeNormalized() {
        PhoneNumber p = new PhoneNumber("+38 (067) 123-45-67");
        assertEquals("+380671234567", p.normalized());
    }

    @Test
    void tooShortPhoneShouldThrowException() {
        assertThrows(ValidationException.class, () -> new PhoneNumber("123"));
    }

    @Test
    void equalityShouldBeByNormalizedValue() {
        PhoneNumber p1 = new PhoneNumber("067-123-45-67");
        PhoneNumber p2 = new PhoneNumber("0671234567");
        assertEquals(p1, p2);
    }
}
