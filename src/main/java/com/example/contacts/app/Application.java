package com.example.contacts.app;

import com.example.contacts.controller.ContactController;
import com.example.contacts.repository.FileContactRepository;
import com.example.contacts.service.CommandManager;
import com.example.contacts.service.ContactService;
import com.example.contacts.observer.AuditLogger;
import com.example.contacts.view.ConsoleView;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class Application {

    public static void main(String[] args) {
        // 1. Initializing the repository (CSV file in the working directory)
        Path repoFile = Path.of("contacts.csv");
        FileContactRepository repository = new FileContactRepository(repoFile);

        // 2. Initializing CommandManager
        CommandManager commandManager = new CommandManager();

        // 2.1. Initialize AuditLogger for command logging
        try {
            AuditLogger logger = new AuditLogger(
                Files.newBufferedWriter(
                    Path.of("audit.log"),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
                ),
                true // alsoConsole = true
            );
            commandManager.setLogger(logger);
            System.out.println("✅ Audit logger initialized: audit.log");
        } catch (IOException e) {
            System.err.println("⚠️ Failed to initialize audit logger: " + e.getMessage());
        }

        // 3. Service initialization
        ContactService service = new ContactService(repository, commandManager);

        // 4. Controller initialization
        ContactController controller = new ContactController(service);

        // 5. Launching the console interface
        ConsoleView view = new ConsoleView(controller);
        view.start();

        // 6. Before leaving — saving data
        service.flush();
    }
}
