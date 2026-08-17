# Cloud File Storage

Multi-user cloud file storage (upload, download, manage files and folders). Prototype: Google Drive.

**Demo:** [http://31.56.208.168:8080/](http://31.56.208.168:8080/)

## Stack

- Java 17, Spring Boot, Maven
- PostgreSQL, Redis (sessions), MinIO
- Docker Compose

## Limits

| Limit | Value |
|-------|--------|
| Single file | up to **50 MB** |
| One multipart request (e.g. folder upload) | up to **500 MB** |
| Parts per request | up to **1000** files |

## Swagger

UI: `/swagger-ui/index.html`  
API docs: `/v3/api-docs`

Available **only for authenticated users** (sign in first).

## Quick start

1. Create `.env` (Compose) and `application-local.properties` in the project root (both are gitignored).
2. `docker compose up -d` (Postgres, Redis, MinIO).
3. Run the app (`mvnw spring-boot:run` or IDE).
4. Open `http://localhost:8080/files/`

Demo uses the same compose file with the app container: `docker compose --profile app up -d`.

Schema is applied by Flyway on startup.
