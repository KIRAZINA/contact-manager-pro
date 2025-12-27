package com.example.contacts.domain.factory;

import com.example.contacts.domain.entity.Contact;
import com.example.contacts.domain.value.Address;
import com.example.contacts.domain.value.Email;
import com.example.contacts.domain.value.PhoneNumber;

import java.util.List;

/**
 * Creates a valid Contact from input data using aggregate invariants.
 * Encapsulates time and status initialization.
 */
public final class ContactFactory {

    private ContactFactory() {}

    public static Contact create(String firstName,
                                 String lastName,
                                 List<PhoneNumber> phones,
                                 List<Email> emails,
                                 Address address) {
        return Contact.createNew(firstName, lastName, phones, emails, address);
    }
}
