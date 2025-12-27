package com.example.contacts.domain.value;

import com.example.contacts.exception.ValidationException;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Immutable email with normalization and basic validation.
 * Stores the normalized address in lowercase for consistent comparison.
 */
public record Email(String value) {

    // Basic pattern sufficient for business validation (not a complete RFC)
    private static final Pattern SIMPLE_EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public Email {
        if (value == null) {
            throw new ValidationException("email", "Email cannot be null");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new ValidationException("email", "Email cannot be empty");
        }
        if (!SIMPLE_EMAIL.matcher(trimmed).matches()) {
            throw new ValidationException("email", "Incorrect email format: " + value);
        }
        value = trimmed.toLowerCase(); // normalization for comparison
    }

    public String normalized() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    // Equality/hash — by normalized()
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Email email)) return false;
        return Objects.equals(value, email.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
