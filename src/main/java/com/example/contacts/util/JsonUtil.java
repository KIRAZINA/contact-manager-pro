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

        // Parse objects considering nested braces (handles arrays and nested objects)
        List<String> objects = splitJsonObjects(json.substring(1, json.length() - 1));
        List<Contact> contacts = new ArrayList<>();
        for (String obj : objects) {
            String normalized = obj.trim();
            if (normalized.isEmpty()) continue;
            if (!normalized.startsWith("{")) normalized = "{" + normalized;
            if (!normalized.endsWith("}")) normalized = normalized + "}";
            Contact c = parseContact(normalized);
            contacts.add(c);
        }
        return contacts;
    }

    /**
     * Splits a JSON array content into individual object strings.
     * Properly handles nested braces to avoid splitting inside nested objects or arrays.
     */
    private static List<String> splitJsonObjects(String json) {
        List<String> objects = new ArrayList<>();
        int braceCount = 0;
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        boolean escapeNext = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escapeNext) {
                current.append(c);
                escapeNext = false;
                continue;
            }

            if (c == '\\') {
                current.append(c);
                escapeNext = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                current.append(c);
                continue;
            }

            if (inString) {
                current.append(c);
                continue;
            }

            if (c == '{') {
                braceCount++;
                current.append(c);
            } else if (c == '}') {
                braceCount--;
                current.append(c);
                if (braceCount == 0 && current.length() > 0) {
                    objects.add(current.toString().trim());
                    current = new StringBuilder();
                }
            } else if (c == ',' && braceCount == 0) {
                // Skip commas between objects
                continue;
            } else {
                current.append(c);
            }
        }

        // Add any remaining content if it's a valid object
        if (current.toString().trim().startsWith("{")) {
            objects.add(current.toString().trim());
        }

        return objects;
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
        String addrJson = map.get("address");
        if (addrJson != null && !addrJson.equals("null") && !addrJson.isBlank()) {
            Map<String, String> addrMap = parseSimpleJsonObject(addrJson);
            String street = addrMap.get("street");
            String city = addrMap.get("city");
            String region = addrMap.get("region");
            String postalCode = addrMap.get("postalCode");
            String country = addrMap.get("country");
            // Only create address if at least one field is non-empty
            if ((street != null && !street.isEmpty()) ||
                (city != null && !city.isEmpty()) ||
                (region != null && !region.isEmpty()) ||
                (postalCode != null && !postalCode.isEmpty()) ||
                (country != null && !country.isEmpty())) {
                address = new Address(street, city, region, postalCode, country);
            }
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

        List<String> pairs = splitTopLevel(body, ',');
        for (String pair : pairs) {
            List<String> kv = splitTopLevel(pair, ':');
            if (kv.size() == 2) {
                String key = kv.get(0).trim().replaceAll("^\"|\"$", "");
                String val = kv.get(1).trim();
                // Don't remove quotes from arrays and objects, only from primitive strings
                if (!val.startsWith("[") && !val.startsWith("{")) {
                    val = val.replaceAll("^\"|\"$", "");
                }
                map.put(key, val);
            }
        }
        return map;
    }

    /**
     * Splits a string by delimiter only at the top level.
     * Does not split inside strings, arrays, or objects.
     */
    private static List<String> splitTopLevel(String str, char delimiter) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int braceDepth = 0;
        int bracketDepth = 0;
        boolean inString = false;
        boolean escapeNext = false;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            if (escapeNext) {
                current.append(c);
                escapeNext = false;
                continue;
            }

            if (c == '\\') {
                current.append(c);
                escapeNext = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                current.append(c);
                continue;
            }

            if (inString) {
                current.append(c);
                continue;
            }

            if (c == '{') {
                braceDepth++;
                current.append(c);
            } else if (c == '}') {
                braceDepth--;
                current.append(c);
            } else if (c == '[') {
                bracketDepth++;
                current.append(c);
            } else if (c == ']') {
                bracketDepth--;
                current.append(c);
            } else if (c == delimiter && braceDepth == 0 && bracketDepth == 0) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            result.add(current.toString().trim());
        }

        return result;
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
