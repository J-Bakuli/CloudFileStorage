# Cloud File Storage

## About the project

Multi-user cloud file storage for uploading, downloading, and managing files. Google Drive is the reference prototype.

This is a learning backend project built with Java and Spring Boot.

## Stack

- Java 17
- Spring Boot
- Maven
- PostgreSQL
- MinIO (S3-compatible storage)
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

Create a `.env` file in the project root (for Docker Compose):

```env
POSTGRES_DB=cloud_storage
POSTGRES_USER=your_user
POSTGRES_PASSWORD=your_password
MINIO_ROOT_USER=your_minio_user
MINIO_ROOT_PASSWORD=your_minio_password
```

Create `src/main/resources/application-local.properties` manually after clone (file is in `.gitignore`):

```properties
spring.datasource.url=jdbc:postgresql://localhost:5433/cloud_storage
spring.datasource.username=your_user
spring.datasource.password=your_password
minio.access-key=your_minio_user
minio.secret-key=your_minio_password
```

Use the same database name, username, and password as in `.env`. MinIO credentials must match `MINIO_ROOT_USER` and `MINIO_ROOT_PASSWORD`.

### 3. Start PostgreSQL and MinIO

```bash
docker compose up -d
```

- PostgreSQL host port: `5433` (container port: `5432`)
- MinIO API: `http://localhost:9000`
- MinIO Console: `http://localhost:9001`

Check that containers are running:

```bash
docker ps
```

You should see `cloud_storage_db` and `cloud_storage_minio`.

### 4. Run the application

Run `CloudFileStorageApplication` from your IDE or:

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

## Swagger
http://localhost:8080/swagger-ui/index.html#/

## Project structure

- `src/main/java` — application code
- `src/main/resources` — configuration files and migrations
- `src/main/resources/db/migration/` — Flyway SQL migrations (database schema)
- `docker-compose.yml` — PostgreSQL and MinIO
- `pom.xml` — Maven dependencies

The database schema is managed by Flyway. Migrations from `db/migration/` are applied automatically when the application starts (after `docker compose up -d`).
