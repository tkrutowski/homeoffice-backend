# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Stack

- Java 25, Spring Boot 4.1.1 (all starters pinned via `springframework-boot.version`); Spring AI 2.0.0 (`spring-ai-starter-model-anthropic`) for LLM integration
- MySQL via Spring Data JPA; Flyway for schema (`spring.jpa.hibernate.ddl-auto=none`)
- Real MySQL server (dev and prod — same DB on webio.pl) is 5.7.38, below the minimum Hibernate 7's default `MySQLDialect` supports (8.0.0) — uses `hibernate-community-dialects` with `MySQLLegacyDialect` instead
- A few modules were renamed/split in Boot 4: `spring-boot-restclient` (RestTemplateBuilder split out of autoconfigure), `spring-boot-starter-aspectj` (replaces `spring-boot-starter-aop`, same content — spring-aspects + aspectjweaver, used by `@Aspect`/`AuditAspect`)
- Lombok annotation processor path is declared explicitly in `maven-compiler-plugin` — classpath auto-detection fails under JDK 25 in this project
- Maven (wrapper: `./mvnw` on Unix, `mvnw.cmd` on Windows)
- AWS SDK v2 (S3, Textract); `ksef-client` SDK from GitHub Packages (`maven.pkg.github.com/CIRFMF/ksef-client-java`)
- Lombok, ModelMapper, Moneta (JavaMoney), jjwt, iText, pdfbox (PDF text extraction), jsoup, vavr
- Login extras: `spring-security-webauthn` (passkeys), `google-api-client` (Google Sign-In ID-token verification)

## Commands

```bash
./mvnw clean verify              # full build + tests
./mvnw spring-boot:run           # run locally
./mvnw clean package             # build JAR (target/homeoffice-<version>.jar)
./mvnw test -Dtest=StringHelperTest          # single test class
./mvnw test -Dtest=ClassName#methodName      # single test method
```

`spring.profiles.active` is set to `prod` in `application.properties`; override with `-Dspring-boot.run.profiles=dev` or `SPRING_PROFILES_ACTIVE=dev` for local runs. Profile-specific files: `application-dev.properties`, `application-prod.properties`.

Required env vars (all profiles): `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET_KEY`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `KSEF_TOKEN`. Optional: `AWS_REGION` (default `eu-central-1`), `BUCKET_NAME` (default `focik-home`), `HOME_URL`, `DEBUG`, `SCHEDULER_CRON`, `GOOGLE_OAUTH_CLIENT_IDS` (Google Sign-In audience allowlist, has a baked-in default — see `docs/GOOGLE_LOGIN.md`), `WEBAUTHN_RP_ID`/`WEBAUTHN_RP_NAME`/`WEBAUTHN_ALLOWED_ORIGINS` (passkeys — see `docs/PASSKEYS_LOGIN.md`; dev profile overrides `rp-id`/`allowed-origins` to `localhost`/`http://localhost:5173`).

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
- **KSef integration** (`goahead/domain/invoice/`): Polish e-invoicing public API. Config via `ksef.config.*` properties (base-uri, token, qr-uri). Token rotates — when KSef calls 401, update `ksef.config.token`. XSD files at repo root (`schemat.xsd`, `ElementarneTypyDanych_v10-0E.xsd`, etc.) define the invoice XML; the JAXB-annotated model (`goahead/domain/invoice/ksef/model/`) is committed source, **not** build-generated — `pom.xml` has no JAXB codegen plugin at all (only the `jakarta.xml.bind-api` runtime dep), so a schema change means manually re-adding a codegen step and hand-merging.
- **AI-assisted extraction**: Claude via Spring AI parses invoice/loan-proposal PDFs and matches bank transactions to firms — `finance/infrastructure/claude/`, `fileService/infrastructure/claude/` (uses `pdfbox` for text extraction first).
- **Security**: JWT (24h access, 7d refresh), `@PreAuthorize("hasAnyAuthority('ROLE_X')")` on controllers. Public endpoints listed in `cors.public-url`. Login also supports Google Sign-In (`POST /api/v1/auth/google` — ID-token verification, never auto-registers accounts, see `docs/GOOGLE_LOGIN.md`) and passkeys/WebAuthn (`/webauthn/**`, `/login/webauthn`, see `docs/PASSKEYS_LOGIN.md`). Passkeys need a **second `SecurityFilterChain`** (`WebAuthnSecurityConfig`, `@Order(1)`, session-based — the main API chain in `SecurityConfig` is `@Order(2)` and stays `STATELESS`); any further extra `SecurityFilterChain` must repeat `.cors(...)` + an explicit `OPTIONS` `permitAll()` itself, or CORS preflight breaks silently (only 401, no CORS headers) even though the global `CorsFilter` bean looks like it should cover everything.

### UseCase Pattern (Ports & Adapters)

Every business operation is exposed through a **UseCase interface** (port) in `domain/*/port/primary/`:

```java
// Port interface (domain layer)
public interface GetCardUseCase {
    Card findById(int id);
    List<Card> findByStatus(ActiveStatus status);
}

// Implementation (domain component, not infrastructure)
@Component
public class CardFacade implements AddCardUseCase, UpdateCardUseCase, 
                                   GetCardUseCase, DeleteCardUseCase {
    private final CardService cardService;
    // Aggregates multiple UseCase interfaces, delegates to service
}

// Usage in controller
@RestController
public class CardController {
    private final GetCardUseCase getCardUseCase; // Inject the port, not the impl
}
```

**When to use Facade vs. direct Service implementation:**
- **Use Facade**: Aggregates multiple UseCase interfaces (CRUD operations) or complex orchestration
- **Use Service implementing UseCase directly**: Single UseCase with domain logic (e.g., `GenerateBankTransactionReportUseCase`)

**Do not use Facade if:**
- You have a single UseCase interface
- Service only delegates to other UseCase ports without aggregation logic
- No orchestration needed between multiple operations

Example of unnecessary Facade:
```java
// ❌ Don't do this
public class BankTransactionFacade implements GenerateBankTransactionReportUseCase {
    private final BankTransactionReportService service;
}

// ✅ Do this instead
public class BankTransactionReportService implements GenerateBankTransactionReportUseCase {
    private final GetBankTransactionUseCase getBankTransactionUseCase;
    // Direct implementation without intermediary
}
```

Controllers always depend on `*UseCase` port interfaces from `domain/`, never on `*Service` or `*Facade` directly.

### Conventions to follow

- Always use Moneta `Money` for currency, never `double` or raw `BigDecimal` in domain.
- ModelMapper is `STRICT` — DTO and domain field names must match exactly; add explicit converters for type mismatches.
- DB schema changes go in `src/main/resources/db/migration/V{next}__{description}.sql` (Flyway) — `{next}` is a plain incrementing integer, **unrelated to the `pom.xml` project version**. Do **not** rely on Hibernate auto-DDL. DB predates Flyway (`baseline-version=0`); the pre-Flyway schema (e.g. `users`) isn't captured in any migration file.
- Long operations follow the async-task pattern; do not block HTTP threads on KSef calls or PDF generation.
- Match the hexagonal layering in any new module — put the port interface in `domain/`, the adapter in `infrastructure/`.
- Inject `*UseCase` port interfaces in controllers, not `*Service` or `*Facade` implementations.

## Versioning

Before creating a git commit, bump the project `<version>` in `pom.xml` (the top-level `<project><version>`, **not** the `<parent><version>` — that one pins the Spring Boot BOM) according to Semantic Versioning, based on what the commit actually changes:

- **MAJOR** (`X.0.0`, reset MINOR and PATCH to 0): breaking changes — removed/renamed public `*UseCase` ports or REST endpoints, incompatible request/response DTO changes, a DB schema change with no backward-compatible migration path.
- **MINOR** (`x.Y.0`, reset PATCH to 0): new backward-compatible functionality — a new endpoint, new `*UseCase`, new module, new optional feature/config flag.
- **PATCH** (`x.y.Z`): bug fixes, internal refactors, dependency bumps, test-only changes — anything that doesn't change a public contract.

Skip the bump when the commit touches only non-shippable files with no effect on the built JAR: `.github/workflows/`, `CLAUDE.md`, other docs (incl. `docs/`), IDE/editor config, `docker-compose.yml`, local dev scripts under `scripts/`.

Bump exactly one level per commit, and fold the `pom.xml` change into the same commit as the rest of the diff — never a separate "bump version" commit. When unsure which level applies (e.g. a change could read as either MINOR or PATCH), ask rather than guessing.

## Documentation upkeep

After a non-trivial change (new dependency, new config/env var, new architectural pattern, a new
gotcha worth remembering, or a fix to something this file already claims incorrectly), check
whether `CLAUDE.md` (or the relevant file under `docs/`) needs updating too, and fold that into
the same commit — don't wait to be asked. Stale docs that confidently state the wrong thing
(wrong versions, wrong class locations, invented behavior — see the `AGENTS.md` cleanup) are
worse than no docs, since they actively mislead the next session instead of just staying silent.
Routine CRUD additions, internal refactors, and bug fixes with no behavior change visible outside
the code don't need a doc update — this is for changes that shift what a future Claude session
needs to know to work in this repo correctly.

## Git commits

Never stage or commit untracked files (new files git doesn't already know about) without the user explicitly naming each one first — this applies even to files Claude itself created earlier in the same session (new source files, new docs, new migrations). When asked to commit, only add already-tracked (modified) files by default; call out any untracked files that logically belong with the change and ask before including them, rather than assuming "commit the changes" covers them. `git add -A`/`git add .` are unsafe here for exactly this reason — this repo's working tree routinely carries unrelated untracked local files (scratch docs, debug scripts, local-only property files with real secrets) that must never end up in a commit.

## Deployment

Dockerfile copies whatever single jar exists at `target/homeoffice-*.jar` (wildcard — doesn't need `APP_VERSION` to find it). CI workflows in `.github/workflows/` (`deploy-to-synology.yml`, `ec2.yml`) handle release; they still read the version from `pom.xml` (`<version>`) via `mvn help:evaluate` to tag the Docker image and pass it as the `APP_VERSION` build-arg (used only as a cosmetic `ENV` in the container — not read anywhere in application code).

## Troubleshooting

- **`./mvnw clean verify` / `HomeOfficeApplicationTests` needs a real, reachable DB**: it's a plain `@SpringBootTest` that boots the full context — same required env vars as `spring-boot:run`, no `application-test.properties` override exists today.
- **PDF generation slow/timing out**: `KsefAsyncWorker`/`PdfAsyncWorker` use plain `@Async` with **no dedicated executor bean** anywhere in the codebase (no `AsyncConfigurer`, no unqualified `Executor`/`TaskExecutor` bean) — they fall back to Spring's default `SimpleAsyncTaskExecutor` (unbounded, one new thread per call). The only explicit thread pool in the app is `emailTaskExecutor` in `emailservice/config/EmailConfig.java` (core 2 / max 5), used only for email. There is no existing pool size to "increase" for KSef/PDF jobs — bounding them would mean adding a new `@Bean Executor`.
- **403 on S3**: check `AwsConfig` startup log first — it validates bucket reachability at boot and logs a clear error before anything else goes wrong.
- **Money mapping silently drops/nulls a field**: missing converter registration in `Config.modelMapper()` for that `Money`/`BigDecimal` field — ModelMapper `STRICT` mode fails closed, not loud.

## AWS CLI / Agent Toolkit

- AWS CLI credentials: use `aws login --profile agent-toolkit` for local AWS access via the Agent Toolkit MCP server (SigV4, browser-based, auto-refreshes).
- Do not use the `default` profile for Agent Toolkit — it holds the application's long-lived access keys (`AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY`).
- AWS MCP server `aws-mcp` is registered in Claude Code (user scope) for AWS documentation/skill lookups and read/write AWS operations via SigV4.