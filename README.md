# GEOSPATIALS API

Spring Boot REST API for learner, expert, and partner early-access registrations.

## Stack

- Java 21
- Spring Boot 3.5.16
- Spring MVC, Bean Validation, Spring Data JPA
- PostgreSQL
- Maven

## Run locally

Set the database variables if your local PostgreSQL configuration differs from the defaults:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/remosens
export DB_USERNAME=postgres
export DB_PASSWORD=your_password
export DDL_AUTO=update
mvn spring-boot:run
```

The API listens on `http://localhost:8080` by default.

## Registration endpoints

- `POST /api/v1/registrations/learners`
- `POST /api/v1/registrations/experts`
- `POST /api/v1/registrations/partners`

Successful registrations return HTTP `201 Created` and a `RegistrationResponse`.
Validation errors return HTTP `400 Bad Request`; duplicate registrations return `409 Conflict`.

## Error response contract

```json
{
  "timestamp": "2026-10-14T00:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "One or more fields are invalid. Correct them and try again.",
  "path": "/api/v1/registrations/learners",
  "fieldErrors": {
    "email": "Enter a valid email address"
  }
}
```

`fieldErrors` is omitted when there are no field-level errors. Unexpected server errors return a generic message; stack traces and internal exception details are logged server-side only.

## Important production notes

- `DDL_AUTO=update` is retained for compatibility with the current early-registration deployment. For production schema management, introduce Flyway or Liquibase and use `DDL_AUTO=validate`.
- Configure `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `CORS_ALLOWED_ORIGINS` through deployment secrets/environment settings. Do not commit real credentials.
- CORS defaults to `http://localhost:5173`; configure the actual frontend origin in deployment.
- The duplicate check improves the normal user experience, while database uniqueness constraints remain the final protection against concurrent duplicate inserts.
- This project intentionally has no authentication layer in the current registration flow. Protect any future administrative or personal-data endpoints before exposing them publicly.

## Modernization changes

- Controllers now return `ResponseEntity` explicitly, with `201 Created` for successful registrations.
- Centralized exception handling consistently formats validation, malformed JSON/enum, database constraint, duplicate-registration, and Spring MVC errors.
- Database integrity errors no longer incorrectly claim that every constraint failure is an email conflict.
- Unexpected failures return a safe client message while details are logged server-side.
- Updated the maintained Spring Boot 3.5 line and Java 21.
