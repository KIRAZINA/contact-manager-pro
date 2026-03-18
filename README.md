# Contact Manager Pro

Console contact manager on pure Java 17 with MVC-style layering, command-based mutations, undo/redo, CSV persistence, and audit logging.

## Features
- Create, view, search, delete, archive, and restore contacts
- Undo and redo for state-changing operations
- Full-text search by name, phone, and email
- CSV persistence in `contacts.csv`
- Audit log output in `audit.log`
- Unit tests with JUnit 5

## Architecture
```text
View (ConsoleView)
  -> Controller (ContactController)
  -> Service layer (ContactService, CommandManager)
  -> Domain model (Contact, value objects, factory)
  -> Persistence (FileContactRepository)
```

## Requirements
- Java 17+
- Maven installed and available as `mvn`
- Docker Desktop or Docker Engine for container builds

## Build And Run
1. Build the project:
   ```bash
   mvn clean package
   ```
2. Run the executable jar:
   ```bash
   java -jar target/contact-manager-pro.jar
   ```

## Run Tests
```bash
mvn test
```

## Docker
Build the image:
```bash
docker build -t contact-manager-pro:local .
```

Run the container interactively:
```bash
docker run -it --rm contact-manager-pro:local
```

Quick smoke run without interactive input:
```bash
docker run --rm contact-manager-pro:local
```

## Project Layout
```text
src/main/java/com/example/contacts
src/test/java/com/example/contacts
Dockerfile
.dockerignore
contacts.csv
pom.xml
README.md
```

## Notes
- The application creates `contacts.csv` automatically if it does not exist.
- The application appends command history entries to `audit.log`.
- In this workspace the Docker image was built and smoke-tested successfully.
