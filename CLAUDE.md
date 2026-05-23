# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

`AGENTS.md` at the repo root contains a more detailed development guide (module deep dives, KSef integration notes, async patterns). Read it for non-trivial work — this file covers the essentials.

## Stack

- Java 21, Spring Boot 3.4.5 (most starters pinned to 3.5.7 via `springframework-boot.version`)
- MySQL via Spring Data JPA; Flyway for schema (`spring.jpa.hibernate.ddl-auto=none`)
- Maven (wrapper: `./mvnw` on Unix, `mvnw.cmd` on Windows)
- AWS SDK v2 (S3, Textract); `ksef-client` SDK from GitHub Packages (`maven.pkg.github.com/CIRFMF/ksef-client-java`)
- Lombok, ModelMapper, Moneta (JavaMoney), jjwt, iText, jsoup, vavr

## Commands

```bash
./mvnw clean verify              # full build + tests
./mvnw spring-boot:run           # run locally
./mvnw clean package             # build JAR (target/homeoffice-<version>.jar)
./mvnw test -Dtest=StringHelperTest          # single test class
./mvnw test -Dtest=ClassName#methodName      # single test method
```

`spring.profiles.active` is set to `prod` in `application.properties`; override with `-Dspring-boot.run.profiles=dev` or `SPRING_PROFILES_ACTIVE=dev` for local runs. Profile-specific files: `application-dev.properties`, `application-prod.properties`.

Required env vars (all profiles): `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET_KEY`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`. Optional: `AWS_REGION` (default `eu-central-1`), `BUCKET_NAME` (default `focik-home`), `HOME_URL`, `DEBUG`, `SCHEDULER_CRON`.

Server runs on port **8077**. Timezone is **Europe/Warsaw** (set in Dockerfile and `hibernate.jdbc.time_zone`) — date logic depends on this.

## Architecture

Hexagonal (ports & adapters). Every business module under `src/main/java/net/focik/homeoffice/<module>/` follows:

```
api/             REST controllers, request/response DTOs, mappers
domain/          entities, *UseCase port interfaces, domain services, domain exceptions
infrastructure/  JPA *DbDto entities, Spring Data repos, UseCase adapters (@Repository)
```

Modules: `goahead` (invoicing/KSef, costs, suppliers/customers), `finance` (purchases, fees, payments, cards, banks, loans, firms), `userservice` (auth, users, roles — also has `application/` layer), `devices`, `library`, `addresses`, `fileService` (S3), `logservice`, `async`, `utils`.

Controllers depend on `*UseCase` interfaces from `domain/`, never on infrastructure adapters directly. Domain stays Spring-free; exceptions are custom `*Exception` classes in `domain/`.

### Key cross-cutting pieces

- **`config/Config.java`**: ModelMapper bean with `STRICT` matching strategy and registered `Money`/`BigDecimal` converters; Spring Security setup; ObjectMapper; password encoder. New `Money` fields require new converters here or mapping will fail silently/partially.
- **`config/AwsConfig.java` + `AwsProperties`**: S3 and Textract clients, presigned URL expiry, bucket config.
- **`async/`**: `AsyncTask` JPA entity tracks long-running jobs (status: `QUEUED`/`RUNNING`/`SUCCEEDED`/`FAILED`/`PARTIAL`). Workers like `KsefAsyncWorker`, `PdfAsyncWorker` use `@Async`. Pattern: controller starts job → returns `jobId` immediately → client polls `/status/{jobId}`.
- **KSef integration** (`goahead/domain/invoice/`): Polish e-invoicing public API. Config via `ksef.config.*` properties (base-uri, token, qr-uri). Token rotates — when KSef calls 401, update `ksef.config.token`. XSD files at repo root (`schemat.xsd`, `ElementarneTypyDanych_v10-0E.xsd`, etc.) define the invoice XML; JAXB generation is currently commented out in `pom.xml`.
- **Security**: JWT (24h access, 7d refresh), `@PreAuthorize("hasAnyAuthority('ROLE_X')")` on controllers. Public endpoints listed in `cors.public-url`.

### Conventions to follow

- Always use Moneta `Money` for currency, never `double` or raw `BigDecimal` in domain.
- ModelMapper is `STRICT` — DTO and domain field names must match exactly; add explicit converters for type mismatches.
- DB schema changes go in `src/main/resources/db/migration/V{next}__{description}.sql` (Flyway). Do **not** rely on Hibernate auto-DDL.
- Long operations follow the async-task pattern; do not block HTTP threads on KSef calls or PDF generation.
- Match the hexagonal layering in any new module — put the port interface in `domain/`, the adapter in `infrastructure/`.

## Deployment

Dockerfile expects `target/homeoffice-${APP_VERSION}.jar`. CI workflows in `.github/workflows/` (`deploy-to-synology.yml`, `ec2.yml`) handle release. Build artifact version comes from `pom.xml` (`<version>`).