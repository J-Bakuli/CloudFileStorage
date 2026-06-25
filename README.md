# Cloud File Storage

## About the project

Multi-user cloud file storage for uploading, downloading, and managing files. Google Drive is the reference prototype.

This is a learning backend project built with Java and Spring Boot.

## Stack

- Java 17
- Spring Boot
- Maven
- PostgreSQL
- Docker / Docker Compose

## Requirements

- JDK 17+
- Docker Desktop (or Docker + Compose)
- Git
- IntelliJ IDEA (optional)

## Quick start

### 1. Clone the repository

```bash
git clone <repository-url>
cd CloudFileStorage
```

### 2. Configure local environment

Create a `.env` file in the project root:

```env
POSTGRES_DB=cloud_storage
POSTGRES_USER=your_user
POSTGRES_PASSWORD=your_password
```

Create `src/main/resources/application-local.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5433/cloud_storage
spring.datasource.username=your_user
spring.datasource.password=your_password
```

> Do **not** commit `.env` or `application-local.properties` — they are listed in `.gitignore`.

### 3. Start PostgreSQL

```bash
docker compose up -d
```

- Host port: `5433` (container port: `5432`)
- Check that the container is running:

```bash
docker ps
```

You should see `cloud_storage_db`.

### 4. Run the application

**IntelliJ IDEA:** run `CloudFileStorageApplication`

**Maven:**

```bash
# Windows
mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

Expected log message:

```text
Started CloudFileStorageApplication
```

## Environment variables

| Variable            | Description   |
|---------------------|---------------|
| `POSTGRES_DB`       | Database name |
| `POSTGRES_USER`     | DB user       |
| `POSTGRES_PASSWORD` | DB password   |

## Project structure

- `src/main/java` — application code
- `src/main/resources` — configuration files and migrations
- `src/main/resources/db/migration/` — Flyway SQL migrations (database schema)
- `docker-compose.yml` — PostgreSQL setup
- `pom.xml` — Maven dependencies

The database schema is managed by Flyway. Migrations from `db/migration/` are applied automatically when the application 
starts (after `docker compose up -d`).
