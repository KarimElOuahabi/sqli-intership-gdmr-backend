# GDMR Backend

REST API for **GDMR**, an occupational health visit management platform. HR schedules medical visits for employees, doctors confirm and run them, and everyone is notified in real time.

Built during my internship at SQLI. The web client lives in [gdmr-frontend](https://github.com/KarimElOuahabi/gdmr-frontend).

## Features

- **Roles:** `ADMIN`, `HR`, `DOCTOR`, `EMPLOYEE`, each with its own permissions enforced in one central security config.
- **Visit negotiation flow:** `REQUESTED → PROPOSED → AWAITING_DOCTOR_CONFIRMATION → SCHEDULED → IN_PROGRESS → COMPLETED` (or `ABSENT`). HR proposes a slot, the employee and the doctor can accept or reject, and every step is kept in a negotiation history.
- **Live notifications** over Server-Sent Events, authenticated with short-lived single-use tickets. Redis pub/sub delivers them across multiple backend instances.
- **Auth:** short-lived JWT access tokens plus opaque refresh tokens in an httpOnly cookie.
- **Documents and profile pictures:** file upload behind a storage port.
- **Visit reminders**, medical history, doctor time slots, and staff management.
- **OpenAPI/Swagger UI** for exploring the API (disabled in the `prod` profile).

## Tech stack

Java 21 · Spring Boot 4 · Spring Security · Spring Data JPA · PostgreSQL 16 · Flyway · Redis · springdoc-openapi · Testcontainers · Gradle · Docker · nginx · GitHub Actions

## Architecture

Hexagonal (ports and adapters), one package per bounded context: `auth`, `doctor`, `document`, `employee`, `timeslot`, `visit`, `notification`, plus `shared` for cross-cutting code.

```
<module>/
├── adapter/in/web        REST controllers and DTOs
├── adapter/out           persistence, security, storage adapters
├── application           services implementing the use cases
└── domain                models, ports (in/out), exceptions
```

Domain models are plain objects, with no JPA or web annotations. The schema is owned entirely by Flyway migrations (`src/main/resources/db/migration`), and Hibernate only validates it (`ddl-auto=validate`).

## Running locally

Requirements: JDK 21, Docker.

```bash
docker-compose up -d postgres redis   # Postgres on :5434, Redis on :6379
./gradlew bootRun                     # API on http://localhost:8080
```

Use `gradlew.bat` on Windows. Swagger UI is at `http://localhost:8080/swagger-ui.html`.

To run two backend instances behind nginx (this exercises the Redis notification fan-out), run `docker-compose up --build`. nginx listens on `:8081`.

Set `NOTIFICATIONS_REDIS_ENABLED=false` to run a single instance without Redis.

### Tests

```bash
./gradlew test
```

Covers security rules, visit state transitions and Flyway migrations (using Testcontainers). CI runs `./gradlew build` on every push.

### Main configuration

| Variable | Purpose | Default |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL connection | local Docker DB |
| `REDIS_HOST`, `REDIS_PORT` | Redis for notification pub/sub | `localhost:6379` |
| `NOTIFICATIONS_REDIS_ENABLED` | Turn Redis fan-out on or off | `true` |
| `COOKIE_SECURE` | Secure flag on the refresh cookie | `false` |

## Related

- Frontend: [gdmr-frontend](https://github.com/KarimElOuahabi/gdmr-frontend)
