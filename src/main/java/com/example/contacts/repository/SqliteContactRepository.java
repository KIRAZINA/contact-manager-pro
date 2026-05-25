package com.example.contacts.repository;

import com.example.contacts.domain.entity.Contact;
import com.example.contacts.domain.enum_.ContactStatus;
import com.example.contacts.domain.value.Address;
import com.example.contacts.domain.value.Email;
import com.example.contacts.domain.value.PhoneNumber;
import com.example.contacts.exception.ValidationException;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * SQLite-backed implementation of {@link ContactRepository}.
 *
 * Improvements over the previous CSV-based approach (see Architectural Review):
 *  - Issue 4.1: ACID transactions replace fragile file-write semantics
 *  - Issue 4.2: Single-row INSERT/UPDATE instead of full O(n) file rewrite
 *  - Issue 4.3: WAL journal mode prevents crash between memory update and flush
 *  - Issue 4.4: Schema enforces structure — malformed "rows" cannot exist
 *  - Issue 5.1/5.2: SQL LIKE search — no token explosion, no in-memory O(n×tokens) scan
 *  - Issue 6.1/6.2: Search logic lives in SQL, not in the domain entity
 *  - Issue 3.1: ReentrantReadWriteLock — concurrent reads no longer block each other
 *  - Issue 3.2: Every read reconstructs a fresh Contact from a ResultSet — no shared
 *               mutable references escape the repository
 */
public final class SqliteContactRepository implements ContactRepository, AutoCloseable {

    // ---------- Schema ----------
    private static final String CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS contacts (
                id            TEXT PRIMARY KEY,
                first_name    TEXT NOT NULL,
                last_name     TEXT NOT NULL,
                phones        TEXT NOT NULL DEFAULT '',
                emails        TEXT NOT NULL DEFAULT '',
                addr_street   TEXT,
                addr_city     TEXT,
                addr_region   TEXT,
                addr_postal   TEXT,
                addr_country  TEXT,
                created_at    TEXT NOT NULL,
                updated_at    TEXT NOT NULL,
                status        TEXT NOT NULL
            )
            """;

    private static final String UPSERT = """
            INSERT INTO contacts
                (id, first_name, last_name, phones, emails,
                 addr_street, addr_city, addr_region, addr_postal, addr_country,
                 created_at, updated_at, status)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT(id) DO UPDATE SET
                first_name   = excluded.first_name,
                last_name    = excluded.last_name,
                phones       = excluded.phones,
                emails       = excluded.emails,
                addr_street  = excluded.addr_street,
                addr_city    = excluded.addr_city,
                addr_region  = excluded.addr_region,
                addr_postal  = excluded.addr_postal,
                addr_country = excluded.addr_country,
                created_at   = excluded.created_at,
                updated_at   = excluded.updated_at,
                status       = excluded.status
            """;

    private static final String SELECT_ALL  = "SELECT * FROM contacts";
    private static final String SELECT_BY_ID = "SELECT * FROM contacts WHERE id = ?";
    private static final String DELETE_BY_ID = "DELETE FROM contacts WHERE id = ?";
    private static final String SEARCH = """
            SELECT * FROM contacts
            WHERE lower(first_name) LIKE ?
               OR lower(last_name)  LIKE ?
               OR lower(phones)     LIKE ?
               OR lower(emails)     LIKE ?
            """;

    // ---------- State ----------
    private final String jdbcUrl;
    /** Single shared connection — protected by the read/write lock. */
    private Connection connection;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    // ---------- Construction ----------

    public SqliteContactRepository(String dbPath) {
        this.jdbcUrl = "jdbc:sqlite:" + dbPath;
        this.connection = openConnection();
        initSchema();
    }

    private Connection openConnection() {
        try {
            Connection conn = DriverManager.getConnection(jdbcUrl);
            try (Statement st = conn.createStatement()) {
                // WAL mode: no crash between memory update and disk write (Issue 4.3)
                st.execute("PRAGMA journal_mode=WAL");
                // Enforce foreign keys for future schema evolution
                st.execute("PRAGMA foreign_keys=ON");
                // Synchronous=NORMAL is safe with WAL and much faster than FULL
                st.execute("PRAGMA synchronous=NORMAL");
            }
            return conn;
        } catch (SQLException e) {
            throw new RuntimeException("Cannot open SQLite database: " + jdbcUrl, e);
        }
    }

    private void initSchema() {
        lock.writeLock().lock();
        try (Statement st = connection.createStatement()) {
            st.execute(CREATE_TABLE);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialise SQLite schema", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ---------- ContactRepository implementation ----------

    @Override
    public Optional<Contact> findById(UUID id) {
        lock.readLock().lock();
        try (PreparedStatement ps = connection.prepareStatement(SELECT_BY_ID)) {
            ps.setString(1, id.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("findById failed for id=" + id, e);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Contact> findAll() {
        lock.readLock().lock();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(SELECT_ALL)) {
            List<Contact> result = new ArrayList<>();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return Collections.unmodifiableList(result);
        } catch (SQLException e) {
            throw new RuntimeException("findAll failed", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Contact> findWhere(Predicate<Contact> predicate) {
        // No SQL equivalent for arbitrary predicate — load all and filter in-memory.
        // With SQLite this is safe: each row is a fresh deserialized object.
        return findAll().stream().filter(predicate).collect(Collectors.toUnmodifiableList());
    }

    @Override
    public List<Contact> fullTextSearch(String query) {
        if (query == null || query.isBlank()) return List.of();
        String pattern = "%" + query.trim().toLowerCase() + "%";
        lock.readLock().lock();
        try (PreparedStatement ps = connection.prepareStatement(SEARCH)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ps.setString(4, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                List<Contact> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
                return Collections.unmodifiableList(result);
            }
        } catch (SQLException e) {
            throw new RuntimeException("fullTextSearch failed for query=" + query, e);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Contact> filterByStatus(ContactStatus status) {
        lock.readLock().lock();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM contacts WHERE status = ?")) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                List<Contact> result = new ArrayList<>();
                while (rs.next()) result.add(mapRow(rs));
                return Collections.unmodifiableList(result);
            }
        } catch (SQLException e) {
            throw new RuntimeException("filterByStatus failed", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Contact> filterByCreatedDateRange(LocalDate fromInclusive, LocalDate toInclusive) {
        // Build query dynamically based on which bounds are provided
        StringBuilder sql = new StringBuilder("SELECT * FROM contacts WHERE 1=1");
        if (fromInclusive != null) sql.append(" AND date(created_at) >= ?");
        if (toInclusive   != null) sql.append(" AND date(created_at) <= ?");

        lock.readLock().lock();
        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int idx = 1;
            if (fromInclusive != null) ps.setString(idx++, fromInclusive.toString());
            if (toInclusive   != null) ps.setString(idx,   toInclusive.toString());
            try (ResultSet rs = ps.executeQuery()) {
                List<Contact> result = new ArrayList<>();
                while (rs.next()) result.add(mapRow(rs));
                return Collections.unmodifiableList(result);
            }
        } catch (SQLException e) {
            throw new RuntimeException("filterByCreatedDateRange failed", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Contact save(Contact contact) {
        lock.writeLock().lock();
        try (PreparedStatement ps = connection.prepareStatement(UPSERT)) {
            Optional<Address> addr = contact.getAddress();
            ps.setString(1,  contact.getId().toString());
            ps.setString(2,  contact.getFirstName());
            ps.setString(3,  contact.getLastName());
            ps.setString(4,  joinPipes(contact.getPhones().stream()
                                              .map(PhoneNumber::normalized)
                                              .collect(Collectors.toList())));
            ps.setString(5,  joinPipes(contact.getEmails().stream()
                                              .map(Email::normalized)
                                              .collect(Collectors.toList())));
            ps.setString(6,  addr.map(Address::street).orElse(null));
            ps.setString(7,  addr.map(Address::city).orElse(null));
            ps.setString(8,  addr.map(Address::region).orElse(null));
            ps.setString(9,  addr.map(Address::postalCode).orElse(null));
            ps.setString(10, addr.map(Address::country).orElse(null));
            ps.setString(11, contact.getCreatedAt().toString());
            ps.setString(12, contact.getUpdatedAt().toString());
            ps.setString(13, contact.getStatus().name());
            ps.executeUpdate();
            return contact;
        } catch (SQLException e) {
            throw new RuntimeException("save failed for contact id=" + contact.getId(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean deleteById(UUID id) {
        lock.writeLock().lock();
        try (PreparedStatement ps = connection.prepareStatement(DELETE_BY_ID)) {
            ps.setString(1, id.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("deleteById failed for id=" + id, e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<UUID> deleteAllById(List<UUID> ids) {
        List<UUID> removed = new ArrayList<>();
        lock.writeLock().lock();
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement ps = connection.prepareStatement(DELETE_BY_ID)) {
                for (UUID id : ids) {
                    ps.setString(1, id.toString());
                    if (ps.executeUpdate() > 0) {
                        removed.add(id);
                    }
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw new RuntimeException("deleteAllById failed", e);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("deleteAllById transaction error", e);
        } finally {
            lock.writeLock().unlock();
        }
        return removed;
    }

    /**
     * No-op with SQLite: every save/delete is immediately durable via WAL.
     * Kept in the interface for compatibility and graceful shutdown signalling.
     */
    @Override
    public void flush() {
        // WAL mode ensures durability without an explicit flush step.
        // A checkpoint is issued here as a courtesy on graceful shutdown.
        lock.writeLock().lock();
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA wal_checkpoint(PASSIVE)");
        } catch (SQLException e) {
            System.err.println("Warning: WAL checkpoint failed — " + e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * No-op with SQLite: the database is always in sync with the in-memory view
     * because there is no separate in-memory cache — every read hits the DB.
     */
    @Override
    public void reload() {
        // Nothing to do — SQLite is the single source of truth.
    }

    // ---------- Mapping helpers ----------

    /** Deserializes a single ResultSet row into a fresh Contact object. */
    private Contact mapRow(ResultSet rs) throws SQLException {
        UUID id             = UUID.fromString(rs.getString("id"));
        String firstName    = rs.getString("first_name");
        String lastName     = rs.getString("last_name");
        List<PhoneNumber> phones = parsePhones(rs.getString("phones"));
        List<Email>       emails = parseEmails(rs.getString("emails"));
        Address address     = parseAddress(rs);
        LocalDateTime createdAt = LocalDateTime.parse(rs.getString("created_at"));
        LocalDateTime updatedAt = LocalDateTime.parse(rs.getString("updated_at"));
        ContactStatus status    = ContactStatus.valueOf(rs.getString("status"));
        return new Contact(id, firstName, lastName, phones, emails,
                           address, createdAt, updatedAt, status);
    }

    private List<PhoneNumber> parsePhones(String cell) {
        if (cell == null || cell.isBlank()) return List.of();
        return Arrays.stream(cell.split("\\|"))
                .filter(s -> !s.isBlank())
                .map(PhoneNumber::new)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<Email> parseEmails(String cell) {
        if (cell == null || cell.isBlank()) return List.of();
        return Arrays.stream(cell.split("\\|"))
                .filter(s -> !s.isBlank())
                .map(Email::new)
                .distinct()
                .collect(Collectors.toList());
    }

    private Address parseAddress(ResultSet rs) throws SQLException {
        String street  = rs.getString("addr_street");
        String city    = rs.getString("addr_city");
        String region  = rs.getString("addr_region");
        String postal  = rs.getString("addr_postal");
        String country = rs.getString("addr_country");
        boolean anyPresent = street != null || city != null || region != null
                          || postal != null || country != null;
        if (!anyPresent) return null;
        try {
            return new Address(street, city, region, postal, country);
        } catch (ValidationException e) {
            // Row-level recovery (Issue 4.4): skip bad address rather than crash
            System.err.println("Warning: skipping corrupt address data — " + e.getMessage());
            return null;
        }
    }

    private String joinPipes(List<String> values) {
        return String.join("|", values);
    }

    @Override
    public void close() {
        lock.writeLock().lock();
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Warning: failed to close SQLite connection — " + e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }
}
