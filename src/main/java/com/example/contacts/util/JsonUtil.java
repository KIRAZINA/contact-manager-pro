package com.example.contacts.util;

import com.example.contacts.domain.entity.Contact;
import com.example.contacts.domain.enum_.ContactStatus;
import com.example.contacts.domain.value.Address;
import com.example.contacts.domain.value.Email;
import com.example.contacts.domain.value.PhoneNumber;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A minimalistic utility for serializing/deserializing Contact in JSON.
 * Uses StringBuilder and basic parsing without external libraries.
 */
public final class JsonUtil {

    private JsonUtil() {}

    // ---------- Export ----------

    public static void writeContacts(List<Contact> contacts, Writer writer) throws IOException {
        writer.write("[\n");
        for (int i = 0; i < contacts.size(); i++) {
            Contact c = contacts.get(i);
            writer.write(toJson(c));
            if (i < contacts.size() - 1) {
                writer.write(",\n");
            }
        }
        writer.write("\n]");
    }

    private static String toJson(Contact c) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":\"").append(c.getId()).append("\",");
        sb.append("\"firstName\":\"").append(escape(c.getFirstName())).append("\",");
        sb.append("\"lastName\":\"").append(escape(c.getLastName())).append("\",");

        sb.append("\"phones\":[");
        sb.append(c.getPhones().stream()
                .map(p -> "\"" + escape(p.normalized()) + "\"")
                .collect(Collectors.joining(",")));
        sb.append("],");

        sb.append("\"emails\":[");
        sb.append(c.getEmails().stream()
                .map(e -> "\"" + escape(e.normalized()) + "\"")
                .collect(Collectors.joining(",")));
        sb.append("],");

        sb.append("\"address\":");
        sb.append(c.getAddress().map(JsonUtil::addressToJson).orElse("null")).append(",");

        sb.append("\"createdAt\":\"").append(c.getCreatedAt()).append("\",");
        sb.append("\"updatedAt\":\"").append(c.getUpdatedAt()).append("\",");
        sb.append("\"status\":\"").append(c.getStatus().name()).append("\"");

        sb.append("}");
        return sb.toString();
    }

    private static String addressToJson(Address a) {
        return "{"
                + "\"street\":\"" + escape(a.street()) + "\","
                + "\"city\":\"" + escape(a.city()) + "\","
                + "\"region\":\"" + escape(a.region()) + "\","
                + "\"postalCode\":\"" + escape(a.postalCode()) + "\","
                + "\"country\":\"" + escape(a.country()) + "\""
                + "}";
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"");
    }

    // ---------- Import ----------

    public static List<Contact> readContacts(Reader reader) throws IOException {
        // For simplicity: we read the entire JSON into a string and parse it manually.
        // In a real project, it is better to use the JSON library, but it is not allowed here.
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[1024];
        int n;
        while ((n = reader.read(buf)) != -1) {
            sb.append(buf, 0, n);
        }
        String json = sb.toString().trim();
        if (json.isEmpty() || json.equals("[]")) return List.of();

        // A very simple parser: we separate objects by “},{” (does not support nesting).
        String[] objects = json.substring(1, json.length() - 1).split("},\\s*\\{");
        List<Contact> contacts = new ArrayList<>();
        for (String obj : objects) {
            String normalized = obj;
            if (!normalized.startsWith("{")) normalized = "{" + normalized;
            if (!normalized.endsWith("}")) normalized = normalized + "}";
            Contact c = parseContact(normalized);
            contacts.add(c);
        }
        return contacts;
    }

    private static Contact parseContact(String json) {
        Map<String, String> map = parseSimpleJsonObject(json);

        UUID id = UUID.fromString(map.get("id"));
        String firstName = map.get("firstName");
        String lastName = map.get("lastName");

        List<PhoneNumber> phones = parseArray(map.get("phones")).stream()
                .map(PhoneNumber::new).toList();

        List<Email> emails = parseArray(map.get("emails")).stream()
                .map(Email::new).toList();

        Address address = null;
        if (map.containsKey("address")) {
            Map<String, String> addrMap = parseSimpleJsonObject(map.get("address"));
            address = new Address(
                    addrMap.get("street"),
                    addrMap.get("city"),
                    addrMap.get("region"),
                    addrMap.get("postalCode"),
                    addrMap.get("country")
            );
        }

        LocalDateTime createdAt = LocalDateTime.parse(map.get("createdAt"));
        LocalDateTime updatedAt = LocalDateTime.parse(map.get("updatedAt"));
        ContactStatus status = ContactStatus.valueOf(map.get("status"));

        return new Contact(id, firstName, lastName, phones, emails, address, createdAt, updatedAt, status);
    }

    // ---------- Auxiliary methods ----------

    private static Map<String, String> parseSimpleJsonObject(String json) {
        Map<String, String> map = new HashMap<>();
        String body = json.trim();
        if (body.startsWith("{")) body = body.substring(1);
        if (body.endsWith("}")) body = body.substring(0, body.length() - 1);

        String[] pairs = body.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                String key = kv[0].trim().replaceAll("^\"|\"$", "");
                String val = kv[1].trim();
                val = val.replaceAll("^\"|\"$", "");
                map.put(key, val);
            }
        }
        return map;
    }

    private static List<String> parseArray(String raw) {
        if (raw == null || raw.isEmpty()) return List.of();
        String body = raw.trim();
        if (body.startsWith("[")) body = body.substring(1);
        if (body.endsWith("]")) body = body.substring(0, body.length() - 1);
        if (body.isBlank()) return List.of();
        String[] parts = body.split(",");
        return Arrays.stream(parts)
                .map(s -> s.trim().replaceAll("^\"|\"$", ""))
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
