# AGENTS.md - HomeOffice Backend Development Guide

Welcome! This guide accelerates your productivity in this Spring Boot backend codebase (parent `spring-boot-starter-parent` 3.4.5, individual starters pinned to 3.5.7 via the `springframework-boot.version` property).

## Project Overview

**HomeOffice** is a Polish business management system handling:
- **Invoicing (KSef)**: Polish e-invoicing public API integration via `ksef-client` SDK
- **Finance tracking**: Cost, loan, fee, payment, and card transaction management
- **Device management**: Computer and device inventory/depreciation
- **User/Role management**: JWT-based authentication with Spring Security
- **Async operations**: Long-running KSef/PDF generation jobs with progress tracking
- **File storage**: AWS S3 integration for document uploads and logs

**Architecture**: Hexagonal (ports/adapters) with selective DDD patterns. Stack: Java 21, MySQL, Spring Data JPA, Flyway migrations.

---

## Critical Architecture Patterns

### 1. Hexagonal Architecture (Primary Pattern)

The codebase organizes modules using ports & adapters. Explore any major domain (e.g., `goahead`) to see this:

```
goahead/
├── api/               # Spring REST controllers + DTOs
├── domain/            # Business logic, UseCase port interfaces, Entities
└── infrastructure/    # Spring Data JPA repositories, DB adapters
```

**Key insight**: Domain logic is decoupled from Spring via UseCase interfaces. Controllers depend on `*UseCase` port interfaces, not service implementations.

**Example pattern** (`InvoiceController.java`):
- Injects `GetInvoiceUseCase`, `AddInvoiceUseCase` interfaces
- Implementation is defined in infrastructure layer as `@Repository` adapter
- This isolation enables testing without Spring context

### 2. DTO Mapping with Specialized Converters

Currency/Money handling requires special converters in `config/Config.java`:
- `MoneyToDoubleConverter`, `BigDecimalToMoneyConverter` (and reverse)
- Use **ModelMapper** `STRICT` matching strategy → fields must match exactly
- **Custom post-converters** handle complex nested mappings (see `InvoiceItemDbDto` → `InvoiceItem`)

When adding new DTOs with Money/BigDecimal fields:
1. Register converter in `Config.modelMapper()` bean
2. Test mapping explicitly—STRICT mode catches mismatches

### 3. Async Task Processing Pattern

Long operations (KSef invoice submission, PDF generation) use **@Async workers**:

```java
@Async
public void processJobAsync(String jobId, List<Integer> invoiceIds) {
    AsyncTask task = asyncTaskService.getJobStatus(jobId);
    // update status → RUNNING → SUCCEEDED/FAILED/PARTIAL
}
```

**Workflow**:
1. Controller calls `KsefJobService`/`PdfJobService` → starts async task
2. Returns `AsyncTaskStartResponse` with `jobId` immediately
3. Client polls `/api/.../status/{jobId}` to track progress
4. AsyncTask table stores status, processed count, error messages

**Key classes**: `AsyncTaskService`, `KsefAsyncWorker`, `PdfAsyncWorker`.

---

## Module Deep Dives

### Finance Module (`finance/`)
- **Controllers**: PurchaseController, FeeController, PaymentController, CardController, BankController, LoanController, FirmController
- Handles income/expense categorization with custom fields
- Money library (Moneta) for precision in calculations—never use plain `double` for currency
- Standard CRUD + filtering; uses Spring Data Pageable

### GoAhead Module (`goahead/`)
**Invoicing core**. Domains:
- **Invoice**: Creation, KSef submission, PDF generation, search/filtering
  - Async job tracking via `KsefJobService` (→ `KsefAsyncWorker`)
  - PDF generation via `PdfJobService` (→ `PdfAsyncWorker`)
- **Cost**: Similar async pattern for batch operations
- **Company/Customer/Supplier**: Master data
- Uses XML schema (JAXB) for KSef API payload construction

**KSef Integration**:
- Config in properties: `ksef.config.base-uri`, `token`, `qr-uri`, `suffix-uri`, `request-timeout`
- `InvoiceKsefDto` wraps invoice XML built from the XSD schemas at the repo root (`schemat.xsd`, `ElementarneTypyDanych_v10-0E.xsd`, `KodyKrajow_v10-0E.xsd`, `StrukturyDanych_v10-0E.xsd`)
- JAXB generation (`jaxb2-maven-plugin`) is currently **commented out** in `pom.xml`; regenerate manually if the schema changes
- Mock/staging APIs available at `api-test.ksef.mf.gov.pl` (commented in properties)
- Token rotates: when KSef returns 401, update `ksef.config.token`

### User Service (`userservice/`)
- Hexagonal: `AppUser` domain entity, `IAppUserRepository` port
- Spring Security integration in `Config.java` (BCrypt encoder, AuthenticationManager)
- Role-based access: `@PreAuthorize("hasAnyAuthority('ROLE_NAME')")`
- JWT tokens: 24h expiry (configurable), 7d refresh token

### File Service (`fileService/`)
- AWS S3 integration via `AwsConfig`
- Multipart upload support (10MB file limit)
- Logging to S3 via `S3LogAppender` (Logback integration)

### Async Module (`async/`)
- `AsyncTask`: JPA entity (UUID jobId, status enum, processed/total counts)
- `AsyncTaskRepository`: Spring Data repo
- `AsyncTaskService`: CRUD + status lookup
- Statuses: `QUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED`, `PARTIAL`

---

## Development Workflows

### Building & Running

```bash
# Maven commands (use mvnw.cmd on Windows)
./mvnw clean verify                                  # Full build + tests
./mvnw spring-boot:run                               # Local run
./mvnw clean package                                 # Build JAR for Docker
./mvnw test -Dtest=StringHelperTest                  # Single test class
./mvnw test -Dtest=ClassName#methodName              # Single test method
```

Server listens on **port 8077**. Timezone is **Europe/Warsaw** (set in Dockerfile and `hibernate.jdbc.time_zone`) — date logic depends on this.

`spring.profiles.active` is set to `prod` in `application.properties`. To run with the dev profile:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
# or
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

**Required environment variables** (all profiles): `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET_KEY`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`. Optional: `AWS_REGION` (default `eu-central-1`), `BUCKET_NAME` (default `focik-home`), `HOME_URL`, `DEBUG`, `SCHEDULER_CRON`.

Public endpoints (no auth) are listed in the `cors.public-url` property.

### Database Migrations

Uses **Flyway** (not Hibernate auto-DDL). Migrations in `src/main/resources/db/migration/`:
- Naming: `V{VERSION}__{Description}.sql` (e.g., `V3_17_0__add_async_tasks.sql`)
- **New schema changes**: Create `V{next}__*.sql` file in migration folder
- Flyway auto-applies on startup; `spring.jpa.hibernate.ddl-auto=none` prevents overwrites

### Testing

JUnit 5 + Spring Test (`spring-boot-starter-test`, `spring-security-test`). Existing tests:
- `HomeOfficeApplicationTests` (context load smoke test)
- `src/test/java/net/focik/homeoffice/utils/StringHelperTest.java`
- `src/test/java/net/focik/homeoffice/goahead/domain/` (domain-level tests)

Run a single test: `./mvnw test -Dtest=StringHelperTest` or `./mvnw test -Dtest=ClassName#methodName`. Use `@SpringBootTest` for integration tests; configure a separate profile via `application-test.properties` if needed.

### Docker Deployment

```dockerfile
# Dockerfile uses Java 21 Alpine (Timezone: Europe/Warsaw, exposes 8077)
# Expects: target/homeoffice-${APP_VERSION}.jar (APP_VERSION matches <version> in pom.xml)

# Build & run:
docker build -t homeoffice:latest --build-arg APP_VERSION=3.16.1 .
docker run -p 8077:8077 \
           -e DB_URL=... -e DB_USERNAME=... -e DB_PASSWORD=... \
           -e JWT_SECRET_KEY=... \
           -e AWS_ACCESS_KEY_ID=... -e AWS_SECRET_ACCESS_KEY=... \
           homeoffice:latest
```

CI workflows in `.github/workflows/` (`deploy-to-synology.yml`, `ec2.yml`) drive release.

---

## Key Dependencies & Integrations

| Dependency | Purpose | Notes |
|-----------|---------|-------|
| `spring-boot-starter-data-jpa` | ORM | With Lombok `@Entity` classes |
| `spring-boot-starter-security` | Auth | JWT + role-based (`@PreAuthorize`) |
| `jjwt` | Token generation | See `Config.java` |
| `ksef-client` | Polish KSef e-invoicing SDK | Pulled from `maven.pkg.github.com/CIRFMF/ksef-client-java` (GitHub Packages) |
| `software.amazon.awssdk:s3` / `s3-transfer-manager` / `textract` | AWS SDK v2 | File upload/download, log appender, invoice OCR |
| `modelmapper` | DTO mapping | Configured in `Config.java` bean — STRICT matching |
| `vavr` | Functional utilities | Option, Try, etc. |
| `jsoup` | HTML parsing | Document extraction |
| `itextpdf` 5.x | PDF generation | Invoice/cost PDF rendering |
| `jackson` | JSON serialization | Configured in `objectMapper()` bean |
| `bouncycastle` (`bcpkix-jdk18on`) | Crypto | KSef signing/PKI |

---

## Common Development Tasks

### Add a New REST Endpoint

1. **API Layer** (`{module}/api/{Entity}Controller.java`):
   ```java
   @PostMapping
   @PreAuthorize("hasAnyAuthority('ROLE_WRITE')")
   ResponseEntity<EntityDto> create(@RequestBody EntityDto dto) {
       Entity domain = mapper.toDomain(dto);
       Entity saved = addUseCase.save(domain);
       return ResponseEntity.ok(mapper.toDto(saved));
   }
   ```

2. **Domain Layer** (`domain/{Entity}.java`):
   - Create entity class with business logic
   - Define `{Entity}UseCase` port interface

3. **Infrastructure** (`infrastructure/jpa/{Entity}Repository*.java`):
   - Implement UseCase as `@Repository` adapter
   - Map domain ↔ DB DTO using ModelMapper

4. **Error Handling**: Extend `ExceptionHandling` base class or throw custom exceptions mapped in `@ExceptionHandler` methods

### Start an Async Job

```java
// In controller
AsyncTaskStartResponse response = jobService.startJob(invoiceIds.size(), "KSEF");
// Returns jobId for client polling
```

**Polling endpoint** (implement):
```java
@GetMapping("/status/{jobId}")
ResponseEntity<AsyncTask> getStatus(@PathVariable String jobId) {
    return ResponseEntity.ok(asyncTaskService.getJobStatus(jobId));
}
```

### Add Database Field

1. Create Flyway migration in `db/migration/V{next}__*.sql`
2. Add field to corresponding `*DbDto` JPA entity
3. Update domain `*` entity if business logic uses field
4. Add ModelMapper converter if Money/custom type
5. Test locally before committing

### Configure for New Environment

Create `application-{env}.properties` in `src/main/resources/`:
```properties
spring.profiles.active={env}
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
security.jwt.secret-key=${JWT_SECRET_KEY}
aws.accessKeyId=${AWS_ACCESS_KEY_ID}
aws.secretKey=${AWS_SECRET_ACCESS_KEY}
```

Pass environment variables at runtime or in `.env` file.

---

## Debugging Tips

### Logging
- Config: `logback-spring.xml` → rolling files to `/logs/homeoffice.log`
- S3 appender: `S3LogAppender` pushes logs to AWS S3 bucket
- Enable debug: `DEBUG=true` environment variable or property

### Common Issues

**KSef token expired**: Update `ksef.config.token` in properties—tokens reset periodically
**PDF generation timeout**: Async workers run in thread pool; check thread pool config in `Config` and increase if needed
**Money conversion fails**: Ensure `Money` fields have registered converters in ModelMapper bean
**403 Forbidden on S3**: AWS credentials or bucket policy issue—check `AwsConfig` startup log for detailed error

### Access Control

**@PreAuthorize hints**:
- `hasAnyAuthority('ROLE_X')` → User must have at least one listed authority
- Use `hasAuthority()` for exact match
- Authority names are stored in Spring Security context after JWT validation

---

## File Structure Quick Reference

```
src/main/java/net/focik/homeoffice/
├── config/              # Spring @Configuration beans, converters, AWS/S3 setup
├── async/               # AsyncTask JPA entity, repository, service
├── {module}/
│   ├── api/             # @RestControllers, DTOs, mappers
│   ├── domain/          # Business entities, UseCase ports, domain services
│   └── infrastructure/  # JPA adapters, repository implementations
├── userservice/         # User/Auth domain (hexagonal)
├── fileService/         # S3 file upload/download
├── logservice/          # Logging controllers
└── utils/
    ├── exceptions/      # @RestControllerAdvice, HttpResponse wrappers
    ├── share/           # Enums (PaymentStatus, PaymentMethod)
    └── ksef/            # KSef HTTP client helpers

src/main/resources/
├── application.properties       # Base config
├── application-{dev,prod}.properties
├── db/migration/                # Flyway SQL migrations
├── schemat.xsd, *.xsd          # XML schemas (JAXB binding)
├── logback-spring.xml          # Logging configuration
└── covers/, prompts/, static/  # Templates, static assets
```

---

## Key Commands for AI Agents

When implementing features, follow these steps:

1. **Understand the domain**: Read UseCase interfaces in `domain/` → understand contracts
2. **Check DTOs**: Verify field names match exactly (ModelMapper STRICT mode)
3. **Map conversions**: Add Money converters if new currency fields
4. **Write controller**: Use `@PreAuthorize` for security, inject UseCase ports
5. **Test async**: If long operation, follow KsefAsyncWorker pattern
6. **Run full build**: `./mvnw clean verify` before commit
7. **Check logs**: Monitor startup for credential/S3 validation errors

---

## Notes for AI Agents

- **Never hardcode credentials**: Use environment variables (`${VAR_NAME}`)
- **Timezone awareness**: Application uses `Europe/Warsaw` timezone (critical for date comparisons)
- **Money precision**: Always use `Money` type from Moneta library; never use `double`
- **Async errors**: AsyncTask stores error details in `async_task_errors` table—query them for debugging
- **XML schemas**: JAXB classes in `generated-sources/jaxb/` are generated from `.xsd` files; regenerate with Maven if schemas change
- **Spring profiles matter**: Dev behavior differs from prod (logging levels, DB endpoints)
- **Hexagonal principle**: Never couple domain logic to Spring; exceptions are custom `*Exception` classes in domain

