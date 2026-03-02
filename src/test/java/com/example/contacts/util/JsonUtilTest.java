package com.example.contacts.util;

import com.example.contacts.domain.entity.Contact;
import com.example.contacts.domain.value.Address;
import com.example.contacts.domain.value.Email;
import com.example.contacts.domain.value.PhoneNumber;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JsonUtil - JSON serialization/deserialization.
 */
class JsonUtilTest {

    @Test
    void writeContactsShouldGenerateValidJson() throws Exception {
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null);

        StringWriter writer = new StringWriter();
        JsonUtil.writeContacts(List.of(c), writer);

        String json = writer.toString();
        assertTrue(json.contains("\"firstName\":\"Ivan\""));
        assertTrue(json.contains("\"lastName\":\"Petrenko\""));
        assertTrue(json.contains("0671234567"));
    }

    @Test
    void writeContactsShouldHandleMultipleContacts() throws Exception {
        Contact c1 = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null);
        Contact c2 = Contact.createNew("Petro", "Ivanenko",
                List.of(new PhoneNumber("0509876543")),
                List.of(new Email("petro@test.com")), null);

        StringWriter writer = new StringWriter();
        JsonUtil.writeContacts(List.of(c1, c2), writer);

        String json = writer.toString();
        assertTrue(json.contains("Ivan"));
        assertTrue(json.contains("Petro"));
    }

    @Test
    void writeContactsShouldHandleAddress() throws Exception {
        Address address = new Address("Main St", "Kyiv", null, "01001", "Ukraine");
        Contact c = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), address);

        StringWriter writer = new StringWriter();
        JsonUtil.writeContacts(List.of(c), writer);

        String json = writer.toString();
        assertTrue(json.contains("\"city\":\"Kyiv\""));
        assertTrue(json.contains("\"country\":\"Ukraine\""));
    }

    @Test
    void writeContactsShouldEscapeSpecialCharacters() throws Exception {
        Contact c = Contact.createNew("Ivan", "Petrenko\"",
                List.of(new PhoneNumber("0671234567")),
                List.of(new Email("ivan@test.com")), null);

        StringWriter writer = new StringWriter();
        JsonUtil.writeContacts(List.of(c), writer);

        String json = writer.toString();
        assertTrue(json.contains("\\\""));
    }

    @Test
    void readContactsShouldParseValidJson() throws Exception {
        String json = """
            [{
                "id": "123e4567-e89b-12d3-a456-426614174000",
                "firstName": "Ivan",
                "lastName": "Petrenko",
                "phones": ["0671234567"],
                "emails": ["ivan@test.com"],
                "address": null,
                "createdAt": "2026-01-01T10:00",
                "updatedAt": "2026-01-01T10:00",
                "status": "ACTIVE"
            }]
            """;

        StringReader reader = new StringReader(json);
        List<Contact> contacts = JsonUtil.readContacts(reader);

        assertEquals(1, contacts.size());
        assertEquals("Ivan", contacts.get(0).getFirstName());
        assertEquals("Petrenko", contacts.get(0).getLastName());
    }

    @Test
    void readContactsShouldParseMultipleContacts() throws Exception {
        String json = """
            [
              {
                "id": "123e4567-e89b-12d3-a456-426614174000",
                "firstName": "Ivan",
                "lastName": "Petrenko",
                "phones": ["0671234567"],
                "emails": ["ivan@test.com"],
                "address": null,
                "createdAt": "2026-01-01T10:00",
                "updatedAt": "2026-01-01T10:00",
                "status": "ACTIVE"
              },
              {
                "id": "123e4567-e89b-12d3-a456-426614174001",
                "firstName": "Petro",
                "lastName": "Ivanenko",
                "phones": ["0509876543"],
                "emails": ["petro@test.com"],
                "address": null,
                "createdAt": "2026-01-02T10:00",
                "updatedAt": "2026-01-02T10:00",
                "status": "ACTIVE"
              }
            ]
            """;

        StringReader reader = new StringReader(json);
        List<Contact> contacts = JsonUtil.readContacts(reader);

        assertEquals(2, contacts.size());
    }

    @Test
    void readContactsShouldHandleEmptyArray() throws Exception {
        StringReader reader = new StringReader("[]");
        List<Contact> contacts = JsonUtil.readContacts(reader);

        assertTrue(contacts.isEmpty());
    }

    @Test
    void readContactsShouldHandleEmptyString() throws Exception {
        StringReader reader = new StringReader("");
        List<Contact> contacts = JsonUtil.readContacts(reader);

        assertTrue(contacts.isEmpty());
    }

    @Test
    void roundTripShouldPreserveData() throws Exception {
        Address address = new Address("Main St", "Kyiv", "Kyivska", "01001", "Ukraine");
        Contact original = Contact.createNew("Ivan", "Petrenko",
                List.of(new PhoneNumber("0671234567"), new PhoneNumber("0501234567")),
                List.of(new Email("ivan@test.com"), new Email("ivan.petrenko@work.com")),
                address);

        // Serialize
        StringWriter writer = new StringWriter();
        JsonUtil.writeContacts(List.of(original), writer);

        // Deserialize
        StringReader reader = new StringReader(writer.toString());
        List<Contact> restored = JsonUtil.readContacts(reader);

        assertEquals(1, restored.size());
        Contact c = restored.get(0);
        assertEquals(original.getFirstName(), c.getFirstName());
        assertEquals(original.getLastName(), c.getLastName());
        assertEquals(original.getPhones().size(), c.getPhones().size());
        assertEquals(original.getEmails().size(), c.getEmails().size());
    }
}
