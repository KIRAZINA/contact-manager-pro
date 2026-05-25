package com.example.contacts.app;

import com.example.contacts.controller.ContactController;
import com.example.contacts.observer.AuditLogger;
import com.example.contacts.repository.FileContactRepository;
import com.example.contacts.repository.SqliteContactRepository;
import com.example.contacts.domain.entity.Contact;
import com.example.contacts.service.CommandManager;
import com.example.contacts.service.ContactService;
import com.example.contacts.view.ConsoleView;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Application entry point.
 *
 * Improvements (see Architectural Review):
 *  - Issue 4.x:  Uses SqliteContactRepository instead of FileContactRepository
 *  - Issue 9.2:  JVM shutdown hook calls service.flush() on SIGTERM / normal exit
 *  - Issue 9.2:  Periodic background flush every 60 s (extra safety net)
 *  - Issue 8.1:  AuditLogger now receives a Path and writes only after successful
 *                persistence (audit is called post-save inside CommandManager)
 *  - Migration:  Detects existing contacts.csv and imports its data into SQLite
 */
public final class Application {

    private static final String DB_FILE  = "contacts.db";
    private static final String CSV_FILE = "contacts.csv";
    private static final String CSV_BAK  = "contacts.csv.bak";

    /** Periodic flush interval in seconds (crash-safe shutdown safety net). */
    private static final int FLUSH_INTERVAL_SECONDS = 60;

    public static void main(String[] args) {
        // -------------------------------------------------------
        // 1. Initialize SQLite repository (Issue 4.x)
        // -------------------------------------------------------
        SqliteContactRepository repository = new SqliteContactRepository(DB_FILE);

        // -------------------------------------------------------
        // 2. Migrate existing contacts.csv → SQLite (if present)
        // -------------------------------------------------------
        migrateCsvIfPresent(repository);

        // -------------------------------------------------------
        // 3. Initialize CommandManager + JSON audit logger (Issue 8.3)
        // -------------------------------------------------------
        CommandManager commandManager = new CommandManager();
        Path auditPath = Path.of("audit.log");
        AuditLogger logger = new AuditLogger(auditPath, true);
        commandManager.setLogger(logger);
        System.out.println("Audit logger initialised: " + auditPath.toAbsolutePath());

        // -------------------------------------------------------
        // 4. Initialize service and controller
        // -------------------------------------------------------
        ContactService service    = new ContactService(repository, commandManager);
        ContactController controller = new ContactController(service);

        // -------------------------------------------------------
        // 5. Crash-safe shutdown hook — flush on SIGTERM (Issue 9.2)
        // -------------------------------------------------------
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[Shutdown] Flushing data...");
            service.flush(); // WAL checkpoint
            System.out.println("[Shutdown] Flush complete.");
        }, "shutdown-flush"));

        // -------------------------------------------------------
        // 6. Periodic background flush every 60 s (Issue 9.2)
        //    Uses a daemon thread so it never prevents JVM exit.
        // -------------------------------------------------------
        ScheduledExecutorService scheduler =
                Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "periodic-flush");
                    t.setDaemon(true);
                    return t;
                });
        scheduler.scheduleAtFixedRate(
                () -> {
                    try { service.flush(); }
                    catch (Exception e) {
                        System.err.println("[Periodic flush] Error: " + e.getMessage());
                    }
                },
                FLUSH_INTERVAL_SECONDS,
                FLUSH_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );

        // -------------------------------------------------------
        // 7. Launch the console interface
        // -------------------------------------------------------
        ConsoleView view = new ConsoleView(controller);
        view.start();

        // -------------------------------------------------------
        // 8. Normal-exit flush (shutdown hook also fires, but explicit is cleaner)
        // -------------------------------------------------------
        scheduler.shutdown();
        service.flush();
        System.out.println("Data saved. Goodbye.");
    }

    /**
     * One-time migration: reads contacts.csv (if it exists and has content) using the
     * legacy FileContactRepository and saves every contact into the new SQLite database.
     * On success, the CSV file is renamed to contacts.csv.bak so the migration does
     * not repeat on the next launch.
     */
    private static void migrateCsvIfPresent(SqliteContactRepository repository) {
        Path csvPath = Path.of(CSV_FILE);
        if (!Files.exists(csvPath)) return;

        try {
            if (Files.size(csvPath) == 0) return;
        } catch (IOException e) {
            return; // can't read size — skip migration
        }

        System.out.println("Detected existing contacts.csv — migrating to SQLite...");
        try {
            FileContactRepository csvRepo = new FileContactRepository(csvPath);
            List<Contact> contacts = csvRepo.findAll();
            int count = 0;
            for (Contact c : contacts) {
                repository.save(c);
                count++;
            }
            // Rename CSV so migration only runs once
            Files.move(csvPath, Path.of(CSV_BAK), StandardCopyOption.REPLACE_EXISTING);
            System.out.printf("Migration complete: %d contact(s) imported. " +
                              "Original saved as %s%n", count, CSV_BAK);
        } catch (Exception e) {
            System.err.println("Warning: CSV migration failed — " + e.getMessage() +
                               ". Continuing with empty SQLite database.");
        }
    }
}
