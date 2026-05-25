FROM maven:3.9.11-eclipse-temurin-17 AS build

WORKDIR /build

COPY pom.xml ./
COPY src ./src

RUN mvn -q -DskipTests package

# -------------------------------------------------------
FROM eclipse-temurin:17-jre
# -------------------------------------------------------

WORKDIR /app

COPY --from=build /build/target/contact-manager-pro.jar ./contact-manager-pro.jar

# Persist the SQLite database and audit log across container restarts.
# Mount a host directory here: -v /host/data:/app/data
# The application writes contacts.db and audit.log to /app by default.
VOLUME ["/app"]

# Issue 9.1 — Health check: verifies the JVM process is alive by checking
# that the SQLite database file was created (i.e., the app initialised fully).
HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
    CMD test -f /app/contacts.db || exit 1

ENTRYPOINT ["java", "-jar", "contact-manager-pro.jar"]
