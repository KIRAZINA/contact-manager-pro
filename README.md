# Contact Manager Pro

A fast, lightweight console-based contact manager built with Java 17. It uses an MVC-style architecture, the Command pattern for undo/redo support, an embedded SQLite database for secure transactional storage, and JSON Lines for structured logging.

## Features
- **Full CRUD operations**: Create, view, search, delete, archive, and restore contacts.
- **Session-Scoped Undo/Redo**: Revert or reapply mutations during the current session (capped at 1000 items to keep memory clean).
- **SQLite Persistence**: Replaces simple files with fully ACID-compliant embedded relational storage (`contacts.db`) in WAL mode.
- **JSON Lines Logging**: Generates structured audit trails (`audit.log`) with automatic rotation when the file reaches 10 MB.
- **CSV Import**: Automatically detects existing legacy `contacts.csv` files on first boot, migrates data to SQLite, and moves the old CSV to `contacts.csv.bak`.

## Architecture
The project follows a standard MVC structure combined with the Command pattern for mutations:

- **View (`ConsoleView`)**: Handles user interactions on the command line.
- **Controller (`ContactController`)**: Dispatches commands and returns friendly responses or validation errors.
- **Service (`ContactService`, `CommandManager`)**: Orchestrates operations and manages the undo/redo stack.
- **Domain (`Contact`, value records)**: Enforces business logic and validates inputs.
- **Repository (`SqliteContactRepository`)**: Handles all SQL database operations and safely maps records to domain entities.

## Requirements
- Java 17+
- Maven
- Docker (optional, for running in containers)

## Getting Started

### Build the Project
Use Maven to clean and package the application:
```bash
mvn clean package
```

### Run the Application
Run the packaged JAR:
```bash
java -jar target/contact-manager-pro.jar
```

### Run Tests
To run all 116 tests (including unit, database, and integration tests):
```bash
mvn test
```

## Running with Docker

### 1. Build the image
```bash
docker build -t contact-manager-pro:local .
```

### 2. Run the container interactively
```bash
docker run -it --rm contact-manager-pro:local
```

### 3. Run with host persistence
To persist the database and audit log across container restarts, mount a host directory to `/app`:
```bash
docker run -it --rm -v $(pwd)/data:/app contact-manager-pro:local
```

## Notes
- Database and audit log files are generated automatically in the current execution folder.
- Validation checks require every contact to have at least one phone number or email address.
- Archived contacts are protected from accidental modifications unless they are explicitly restored first.
