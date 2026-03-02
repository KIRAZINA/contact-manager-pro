# Contact Manager Pro — Core Java 17, Clean MVC, Command, Undo/Redo

## 🔹 Project Overview
**Contact Manager Pro** is a console-based business application built with pure **Java 17**.  
It demonstrates:
- mature use of Core Java
- architectural thinking
- design patterns
- testability
- engineering discipline

Frameworks are **forbidden**. Only the standard Java library is allowed.

---

## 🎯 Features
- CRUD operations for contacts
- Archive / restore contacts
- Full-text search (name, email, phone)
- Sorting (multi-parameter via `Comparator.thenComparing`)
- Filtering (by status, by creation date)
- Undo / Redo for all mutations
- Batch operations (mass delete / archive)
- Input validation (custom exceptions)
- Import / Export (CSV / JSON)
- Audit logging (Observer)

---

## 🧱 Architecture
```
View (ConsoleView)
↓ 
Controller (ContactController)
↓
Application Services (ContactService, CommandManager)
↓
Domain Model (Contact, Value Objects, Factory)
↓
Persistence (FileContactRepository)
```

### Clear boundaries:
- Domain **does not know** about View
- Persistence **does not know** about Controller
- View **contains no business logic**

---

## 🧩 Design Patterns
- **Command Pattern** — every state change = command (`execute()` / `undo()`)
- **Repository Pattern** — interface + file-based implementation
- **Factory** — creation of complex contact objects
- **Strategy** — sorting / filtering
- **Observer** — audit logging of user actions

---

## ⚙️ Core Java Features
- `record` for value objects
- `sealed interfaces` for commands
- `Deque` for undo/redo stacks
- `Streams + Collectors`
- `Optional` instead of `null`
- `Comparator.thenComparing`
- `Collections.unmodifiableList`
- Defensive copies
- `NIO` (`Files`, `Path`)
- `java.time`
- `UUID`

---

## 📂 Project Structure

```
com.example.contacts
├── app
│   └── Application.java
├── controller
│   └── ContactController.java
├── domain
│   ├── entity
│   │   └── Contact.java
│   ├── value
│   │   ├── PhoneNumber.java
│   │   ├── Email.java
│   │   └── Address.java
│   ├── enum
│   │   └── ContactStatus.java
│   └── factory
│       └── ContactFactory.java
├── service
│   ├── ContactService.java
│   └── CommandManager.java
├── command
│   ├── Command.java
│   ├── CreateContactCommand.java
│   ├── UpdateContactCommand.java
│   ├── DeleteContactCommand.java
│   ├── ArchiveContactCommand.java
│   └── RestoreContactCommand.java
├── repository
│   ├── ContactRepository.java
│   └── FileContactRepository.java
├── view
│   └── ConsoleView.java
├── strategy
│   ├── SortStrategy.java
│   └── FilterStrategy.java
├── observer
│   └── AuditLogger.java
├── util
│   ├── CsvUtil.java
│   └── JsonUtil.java
└── exception
    ├── ValidationException.java
    └── ContactNotFoundException.java
```

---

## ▶️ Run Instructions
1. Build the project:
   ```bash
   mvn clean install
   ```
2. Run:
   ```bash
   java -jar target/contact-manager-pro.jar
   ```

---

## 📌 CLI Menu Example
```
=== Contact Manager Pro ===
Menu:
1. Add contact
2. List contacts
3. Search
4. Delete contact
5. Archive contact
6. Restore contact
7. Undo
8. Redo
9. Exit
```

### Example:
```
> 1
First name: Ivan
Last name: Petrenko
Phones: 0671234567
Emails: ivan@test.com
Address: Main St, Kyiv, , , Ukraine
Contact created: 123e4567-e89b-12d3-a456-426614174000
```

---

## 🧪 Testing
- **JUnit 5** (`junit-jupiter-api`, `junit-jupiter-engine`)
- **105+ tests** covering:
    - Value Objects (`Email`, `PhoneNumber`, `Address`)
    - Entity (`Contact`)
    - Domain Factory (`ContactFactory`)
    - Command pattern (`CommandManager`, `CommandIntegrationTest`)
    - Repository (`FileContactRepository`, `FileContactRepositoryExtendedTest`)
    - Services (`ContactService`)
    - Utilities (`CsvUtil`, `JsonUtil`)
    - Observer (`AuditLogger`)

Run tests:
```bash
mvn test
```

---

## 🧭 Engineering Mindset
- Atomic commits, meaningful history
- git-flow (feature / main)
- README with architectural decisions
- Avoid over-engineering
- Readable code > “clever” code
