# Backend

Java 21 · Spring Boot 3 · Maven · PostgreSQL · Flyway · Spring AI (Ollama).

## Package layout

```
src/main/java/com/marcura/exchangerate/
├── config/
├── web/
│   ├── api/          # OpenAPI interface definitions
│   └── dto/          # Request/response records
├── service/
├── repository/
├── domain/
├── client/           # Fixer.io HTTP client
└── job/              # Scheduled ingestion (ShedLock)
```

## Bootstrap

Generate the app (from repo root):

```bash
# Example: Spring Initializr or start.spring.io
# Dependencies: Web, JPA, Validation, PostgreSQL, Flyway, Actuator
```

Then move sources under `com.marcura.exchangerate` and add Flyway scripts under `src/main/resources/db/migration/`.

## Run

```bash
docker compose up db
./mvnw spring-boot:run
```
