package com.example.contacts.domain.value;

import com.example.contacts.exception.ValidationException;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Immutable address. All fields are optional, but if Address is present,
 * at least one field must be non-empty.
 */
public record Address(String street, String city, String region,
                      String postalCode, String country) {

    public Address {
        String s = normalize(street);
        String c = normalize(city);
        String r = normalize(region);
        String p = normalize(postalCode);
        String co = normalize(country);

        boolean anyNonEmpty =
                notEmpty(s) || notEmpty(c) || notEmpty(r) || notEmpty(p) || notEmpty(co);

        if (!anyNonEmpty) {
            throw new ValidationException("address", "The address cannot be completely empty.");
        }

        street = s;
        city = c;
        region = r;
        postalCode = p;
        country = co;

        // Basic length limits — anti-spam/illogical values
        enforceMaxLen("street", street, 200);
        enforceMaxLen("city", city, 100);
        enforceMaxLen("region", region, 100);
        enforceMaxLen("postalCode", postalCode, 20);
        enforceMaxLen("country", country, 100);
    }

    private static String normalize(String s) {
        if (s == null) return null;
        String t = s.trim().replaceAll("\\s{2,}", " ");
        return t.isEmpty() ? null : t;
    }

    private static boolean notEmpty(String s) {
        return s != null && !s.isEmpty();
    }

    private static void enforceMaxLen(String field, String val, int max) {
        if (val != null && val.length() > max) {
            throw new ValidationException(field, "The maximum length for " + field);
        }
    }

    @Override
    public String toString() {
        return Stream.of(street, city, region, postalCode, country)
                .filter(Objects::nonNull)       // skip null values
                .map(String::trim)              // trim whitespace
                .filter(s -> !s.isEmpty())      // skip empty strings
                .collect(Collectors.joining(", "));
    }


    private static String nonNull(String s) {
        return s == null ? "" : s;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Address address)) return false;
        return Objects.equals(street, address.street)
                && Objects.equals(city, address.city)
                && Objects.equals(region, address.region)
                && Objects.equals(postalCode, address.postalCode)
                && Objects.equals(country, address.country);
    }

    @Override
    public int hashCode() {
        return Objects.hash(street, city, region, postalCode, country);
    }
}
