package com.example.contacts.domain.value;

import com.example.contacts.exception.ValidationException;

import java.util.Objects;

/**
 * Immutable phone number in canonical form.
 * Normalization: remove spaces/separators, allow leading ‘+’.
 * The minimum length after normalization is 7 characters, and the maximum is 20 characters.
 */
public record PhoneNumber(String value) {

    public PhoneNumber {
        if (value == null) {
            throw new ValidationException("phone", "The phone number cannot be null");
        }
        String canonical = normalize(value);
        if (canonical.isEmpty()) {
            throw new ValidationException("phone", "The phone cannot be empty");
        }
        if (!isValidCanonical(canonical)) {
            throw new ValidationException("phone", "Incorrect phone number: " + value);
        }
        value = canonical;
    }

    public static String normalize(String raw) {
        String trimmed = raw.trim();
        // Leave only numbers and leading plus signs, remove other symbols
        StringBuilder sb = new StringBuilder();
        boolean leadingPlusKept = false;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '+' && !leadingPlusKept && sb.length() == 0) {
                sb.append(c);
                leadingPlusKept = true;
            } else if (Character.isDigit(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static boolean isValidCanonical(String s) {
        int len = s.startsWith("+") ? s.length() - 1 : s.length();
        return len >= 7 && len <= 20;
    }

    public String normalized() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhoneNumber that)) return false;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
