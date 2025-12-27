package com.example.contacts.app;

import com.example.contacts.controller.ContactController;
import com.example.contacts.repository.FileContactRepository;
import com.example.contacts.service.CommandManager;
import com.example.contacts.service.ContactService;
import com.example.contacts.view.ConsoleView;

import java.nio.file.Path;

public final class Application {

    public static void main(String[] args) {
        // 1. Initializing the repository (CSV file in the working directory)
        Path repoFile = Path.of("contacts.csv");
        FileContactRepository repository = new FileContactRepository(repoFile);

        // 2. Initializing CommandManager
        CommandManager commandManager = new CommandManager();

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
