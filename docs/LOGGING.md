# Logowanie aplikacji (zapis, odczyt, podgląd na żywo, poziomy)

Dokumentacja całego obiegu logów backendu: jak logi są zapisywane, gdzie trafiają, jak API je odczytuje
(historia z S3, podgląd „na żywo”) i jak tymczasowo zmienić poziom logowania (np. na `DEBUG`).
Kod: `src/main/java/net/focik/homeoffice/logservice/` oraz `config/S3LogAppender.java`,
`src/main/resources/logback-spring.xml`. Prompt dla frontendu z opisem API:
[`LOGGING_FRONTEND_PROMPT.md`](LOGGING_FRONTEND_PROMPT.md).

## Zasada działania — najważniejsze

- Logi **zapisuje logback** (nie moduł `logservice`) do trzech miejsc: konsola, lokalny plik z rotacją
  (nietrwały, 7 dni) i — poza profilem `dev` — **S3**, które jest źródłem prawdy dla historii.
- Historię **czyta `logservice` z S3** (wszystkie instancje w jednym bucketcie, rozróżniane nazwą instancji w
  kluczu obiektu). Na profilu `dev` czyta lokalne pliki, bo `dev` nie wysyła logów do S3.
- **Podgląd na żywo** (`/live`) czyta bufor w pamięci procesu — dotyczy **tylko instancji, do której frontend jest
  połączony**. Opóźnienie S3 wynosi do ok. 5 minut, dlatego „na żywo” nie idzie przez S3.
- **Zmiana poziomu logów** (`/levels`) jest tymczasowa (TTL 15 min domyślnie, max 24 h), tylko dla wybranych
  prefiksów loggerów, tylko dla `ROLE_ADMIN` i również tylko na obsługującej instancji.
- Format zapisu **nie może się rozjechać z parserem** (`LogParser`). Historycznie parser oczekiwał formatu
  Spring Boota (`PID ---`), a logback zapisywał własny wzorzec, więc API zwracało puste listy bez żadnego błędu.

## Przegląd

```
                         ┌──────────── zapis (logback) ─────────────┐
 kod aplikacji ── SLF4J ─┤ CONSOLE                                  │
                         │ FILE  ── homeoffice.log(+ .gz, 7 dni)    │  lokalnie, nietrwałe
                         │ ASYNC_S3 ── S3LogAppender ── batch ──────┼─► s3://<bucket>/logs/homeoffice-<data>-<epoch>-<instance>.log
                         │ LIVE_LOGS ── bufor kołowy 2000 wpisów    │  pamięć procesu (wszystkie profile)
                         └──────────────────────────────────────────┘

 odczyt (API):   GET /api/v1/logs, /logs/date  ──►  LogsRepository ──► S3LogsRepositoryAdapter (profil !dev)
                                                                    └─► FileLogsRepositoryAdapter (profil dev)
                 GET /api/v1/logs/live         ──►  LiveLogSource  ──► LogbackLiveLogAdapter (bufor w pamięci)
                 GET/PUT/DELETE /api/v1/logs/levels[/loggers] ─► LogLevelControl ─► SpringLogLevelControl (LoggingSystem)
```

## Zapis logów

### Format

`logback-spring.xml`, wzorzec:

```
%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
```

Przykład: `2026-09-25 10:19:34.704 [http-nio-8077-exec-3] INFO  n.f.h.logservice.api.LogsController - komunikat`.
Wyjątki i stacktrace'y są dopisywane przez logback w kolejnych liniach (`java.lang.X: ...`, `\tat ...`) — parser skleja
je z poprzednim wpisem. Czas jest w strefie JVM (**Europe/Warsaw**, ustawiona w Dockerfile).

### Appendery i profile

| Appender | Profil | Opis |
|---|---|---|
| `CONSOLE` | wszystkie | stdout kontenera |
| `FILE` | wszystkie | `RollingFileAppender`, plik z `logging.file.name`, rotacja dzienna do `<plik>.YYYY-MM-DD.gz`, `maxHistory=7` |
| `ASYNC_S3` → `S3` | tylko `!dev` | `S3LogAppender` opakowany w `AsyncAppender` (kolejka 500, `discardingThreshold=0`) |
| `LIVE_LOGS` | wszystkie | `LogbackLiveLogAdapter` dopinany programowo do root loggera przy starcie aplikacji |

Root logger ma poziom `INFO`; `logging.level.*` z properties (np. w `application-dev.properties`) jest nakładane
przez Spring Boot po załadowaniu `logback-spring.xml`.

### S3LogAppender

- Zbiera sformatowane wpisy w buforze i wysyła je **partiami**: co **100 wpisów** (`batchSize`) albo co **300 s**
  (`flushIntervalSeconds`, ustawione w `logback-spring.xml`; domyślnie w klasie 60 s), oraz przy zamykaniu aplikacji.
- **Żadne I/O sieciowe nie dzieje się w wątku logującym** — wysyła wyłącznie własny wątek `s3-log-appender`.
- Partia, której nie uda się wysłać, **wraca na początek bufora** i jest ponawiana przy następnym flushu
  (limit ok. 5 mln znaków; po przekroczeniu odrzucane są najstarsze wpisy). Błędy trafiają do statusów logbacka
  (`addError`), nie do `System.out`.
- Klucz obiektu: `logs/homeoffice-YYYY-MM-DD-<epochMillis>-<instance>.log`
  - `YYYY-MM-DD` — dzień **wysyłki** (nie musi być dniem wszystkich wpisów w pliku),
  - `<epochMillis>` — moment wysyłki; plik zawiera wpisy od poprzedniego flusha do tego momentu,
  - `<instance>` — z env `APP_INSTANCE` (dozwolone znaki `[A-Za-z0-9_.-]`, reszta → `_`; brak/pusta → `unknown`).
    Sufiks chroni też przed nadpisaniem obiektu przez dwie instancje wysyłające w tej samej milisekundzie.
  - Obiekty sprzed wprowadzenia instancji nie mają sufiksu i są odczytywane jako `unknown`.
- Poświadczenia: `AWS_PROFILE` (jeśli ustawione) albo `DefaultCredentialsProvider` (m.in.
  `AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY`). Region z `aws.region` (domyślnie `eu-central-1`), bucket z
  `aws.bucket.name` (domyślnie `focik-home`).

### Lokalny plik

Ścieżka pochodzi z `logging.file.name` / `logging.file.path` zależnie od profilu (`application.properties`:
`/logs`, `application-prod.properties`: `/app/logs`, `application-dev.properties`: ustawiane lokalnie).
Plik jest **nietrwały** w kontenerze bez wolumenu i trzymany tylko 7 dni, więc nie jest źródłem historii — jest
kopią awaryjną i źródłem odczytu na profilu `dev`. Zalecana ścieżka dla `dev`: `./logs/homeoffice.log`
(katalog `logs/` jest w `.gitignore`).

## Odczyt historii

`GET /api/v1/logs` (dziś) i `GET /api/v1/logs/date` (zakres) idą przez port `LogsRepository`:

- **`S3LogsRepositoryAdapter`** (profil `!dev`):
  1. Dla każdego dnia od `from` do `to + 1 dzień` listuje obiekty z prefiksem `logs/homeoffice-<dzień>-`
     (dzień po `to` może zawierać pierwszy flush z wpisami sprzed `to`).
  2. Z klucza wyciąga epoch i instancję. Odrzuca obiekty spoza okna `[from, to + 6 min]` (6 min = 300 s flusha +
     zapas) oraz — gdy podano `instance` — obiekty innych instancji, **bez ich pobierania**.
  3. Pobiera resztę kolejno (rosnąco po epochu), parsuje `LogParser`-em, filtruje każdy wpis po czasie
     (`from ≤ t < to`) i poziomie, dopisuje `instance` z klucza, przerywa po przekroczeniu `limit`.
  4. Sortuje rosnąco po czasie. Przy przekroczeniu limitu zwraca **najwcześniejsze** wpisy i `truncated = true`.
- **`FileLogsRepositoryAdapter`** (profil `dev`): czyta bieżący `homeoffice.log` i archiwa `.gz` z katalogu
  `logging.file.path`; nie zna pojęcia instancji (`instance` = `null`, parametr `instance` jest ignorowany).
- **Błędy odczytu nie są połykane** — błąd S3 (np. brak `s3:ListBucket`) daje `500` z komunikatem, a nie pustą listę.
- **Limity** (serwis domenowy): `limit` domyślnie 1000, maks. 5000; zakres `to − from` maks. 7 dni;
  `from` musi być wcześniejsze niż `to`.
- **Opóźnienie:** wpisy z ostatnich kilku minut (do najbliższego flusha) nie są jeszcze w S3.

### LogParser

`infrastructure/LogParser.java` — regex dopasowany do wzorca logbacka (poziomy `TRACE|DEBUG|INFO|WARN|ERROR`).
Linie niepasujące do początku wpisu są doklejane (`\n`) do poprzedniego wpisu (stacktrace, wieloliniowe komunikaty);
linie przed pierwszym poprawnym wpisem są pomijane. **Każda zmiana wzorca w `logback-spring.xml` wymaga zmiany
regexa i `LogParserTest`.**

## Podgląd na żywo (tail)

- `LogbackLiveLogAdapter` to appender logbacka trzymający **ostatnie 2000 wpisów** (`logs.live.buffer-size`) w
  buforze kołowym; każdy wpis dostaje rosnący numer `seq`. Bufor jest pusty po restarcie aplikacji.
- Nie zależy od plików ani S3, więc działa tak samo na każdym profilu. Wpisy zawierają `instance` z `APP_INSTANCE`.
- Logi samego `LogsController` są wykluczone (inaczej każdy poll dopisywałby wpisy do bufora).
- Klient **odpytuje co 1–2 s** z kursorem:
  - pierwsze zapytanie bez `after` → ostatnie `limit` wpisów i `cursor`,
  - kolejne `after=<cursor>` z poprzedniej odpowiedzi → tylko nowe wpisy.

| Pole odpowiedzi | Znaczenie |
|---|---|
| `cursor` | numer ostatnio przejrzanego wpisu; **przesuwa się także poza wpisy odfiltrowane po poziomie**; przekazać jako `after` |
| `gap` | `true` = część wpisów przepadła: kursor starszy niż zawartość bufora albo restart aplikacji (kursor > aktualny numer). Klient powinien pokazać przerwę w widoku |
| `hasMore` | `true` = nowych wpisów jest więcej niż `limit`; kursor stoi na ostatnim zwróconym wpisie, można od razu odpytać ponownie |

Po restarcie (kursor większy niż aktualny numer) endpoint zwraca ogon bufora z `gap = true`.

## Zmiana poziomu logów

`LogLevelsService` + `SpringLogLevelControl` (Spring `LoggingSystem`, na produkcji logback).

- `PUT` ustawia poziom loggera, zapamiętuje poziom skonfigurowany **przed pierwszą zmianą** (`null` = dziedziczony)
  i planuje automatyczny powrót po TTL. Ponowna zmiana tego samego loggera zastępuje poziom i przedłuża TTL, ale
  wraca się do stanu sprzed pierwszej zmiany. `DELETE` przywraca natychmiast.
- **Dozwolone loggery** to nazwy równe prefiksowi z `logs.levels.allowed-prefixes` albo zaczynające się od
  `prefiks.` (domyślnie `net.focik.homeoffice`, `org.springframework.security`, `org.hibernate.SQL`,
  `software.amazon.awssdk`). **`ROOT` jest wykluczony** — `DEBUG` na root zalewa bufor live i S3 oraz może ujawnić
  dane wrażliwe (treść zapytań do KSeF/Claude'a, SQL) w logach trzymanych do 90 dni.
- **Lista loggerów do wyboru** (`GET /levels/loggers`): `SpringLogLevelControl.getLoggers()` czyta
  `LoggingSystem.getLoggerConfigurations()`, a `LogLevelsService.getLoggers(prefix)` zostawia tylko te pod
  dozwolonymi prefiksami (opcjonalnie zawężone do `prefix`). Logback tworzy logger dopiero przy pierwszym
  `LoggerFactory.getLogger` (`@Slf4j` = załadowanie klasy), więc klasy jeszcze niezaładowane nie są na liście.
- TTL: 1 min – 24 h (`logs.levels.default-ttl-minutes` = 15, `logs.levels.max-ttl-minutes` = 1440).
- Każda zmiana i przywrócenie jest logowane na `INFO` (kto, jaki logger, jaki poziom, do kiedy).
- Stan jest **w pamięci procesu** — restart = ustawienia domyślne; przy zamykaniu aplikacji nadpisania są cofane.
- Poziomy `FATAL`/`OFF` (poza `LogLevel`) są traktowane jak brak jawnie ustawionego poziomu przy zapamiętywaniu
  „poprzedniego” poziomu (w repo nikt ich nie ustawia).

## API

Wszystkie ścieżki pod `/api/v1/logs`, wymagają JWT (`Authorization: Bearer ...`). Daty i czasy w odpowiedziach to
czas lokalny **Europe/Warsaw bez strefy** (`yyyy-MM-dd'T'HH:mm:ss.SSS` we wpisach logów; w polach `setAt`/`revertsAt`
ISO-8601 z dowolną liczbą cyfr ułamka sekundy). Poziomy: `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`.

| Metoda i ścieżka | Uprawnienia | Opis |
|---|---|---|
| `GET /logs` | `LOGS_READ`, `LOGS_READ_ALL` lub `ROLE_ADMIN` | logi od początku dzisiejszego dnia do teraz |
| `GET /logs/date` | j.w. | logi z zakresu `[from, to)` |
| `GET /logs/live` | j.w. | logi „na żywo” bieżącej instancji (polling z kursorem) |
| `GET /logs/levels` | `ROLE_ADMIN` | konfiguracja i aktywne nadpisania poziomów |
| `GET /logs/levels/loggers` | `ROLE_ADMIN` | lista istniejących loggerów (pakiety/klasy) do wyboru |
| `PUT /logs/levels` | `ROLE_ADMIN` | tymczasowa zmiana poziomu loggera |
| `DELETE /logs/levels/{logger}` | `ROLE_ADMIN` | natychmiastowe przywrócenie poziomu |

### `GET /logs` i `GET /logs/date`

| Parametr | Endpoint | Opis |
|---|---|---|
| `from`, `to` | `/date` | wymagane, `yyyy-MM-ddTHH:mm:ss` (czas lokalny), `from` włącznie, `to` wyłącznie; `from < to`, zakres ≤ 7 dni |
| `levels` | oba | opcjonalne; `levels=ERROR,WARN` albo powtórzony parametr; brak = wszystkie |
| `limit` | oba | opcjonalne; domyślnie 1000, maks. 5000 (większe jest obcinane, ≤ 0 → 1) |
| `instance` | oba | opcjonalne; nazwa instancji (bez rozróżniania wielkości liter), np. `ec2`; `unknown` = stare obiekty; brak = wszystkie |

Odpowiedź `200`:

```json
{
  "entries": [
    {
      "timestamp": "2026-09-25T10:19:34.704",
      "level": "ERROR",
      "thread": "http-nio-8077-exec-2",
      "logger": "o.a.c.c.C.[.[.[.[dispatcherServlet]",
      "message": "Servlet.service() threw exception\nio.jsonwebtoken.ExpiredJwtException: JWT expired\n\tat ...",
      "instance": "ec2"
    }
  ],
  "truncated": false
}
```

Wpisy są posortowane **rosnąco po czasie**. `truncated = true` oznacza, że wpisów było więcej niż `limit` (zwracane są
najwcześniejsze). `instance` jest `null` na profilu `dev`.

### `GET /logs/live`

Parametry: `after` (opcjonalny kursor, `Long ≥ 0`), `levels`, `limit` (domyślnie 200, maks. 2000).

```json
{
  "instance": "ec2",
  "entries": [ { "timestamp": "...", "level": "INFO", "thread": "...", "logger": "...", "message": "...", "instance": "ec2" } ],
  "cursor": 1234,
  "gap": false,
  "hasMore": false
}
```

Znaczenie `cursor`/`gap`/`hasMore` — patrz sekcja „Podgląd na żywo”.

### `GET /logs/levels`

```json
{
  "instance": "ec2",
  "rootLevel": "INFO",
  "allowedLoggers": ["net.focik.homeoffice", "org.springframework.security", "org.hibernate.SQL", "software.amazon.awssdk"],
  "defaultTtlMinutes": 15,
  "maxTtlMinutes": 1440,
  "overrides": [
    {
      "logger": "net.focik.homeoffice.finance",
      "level": "DEBUG",
      "previousLevel": null,
      "setAt": "2026-09-25T10:00:00.123",
      "revertsAt": "2026-09-25T10:15:00.123",
      "changedBy": "admin"
    }
  ]
}
```

`previousLevel = null` oznacza, że logger dziedziczył poziom (po TTL wraca do dziedziczenia).

### `GET /logs/levels/loggers?prefix=`

Lista loggerów (pakietów i klas) istniejących na tej instancji, tylko pod `allowedLoggers`, posortowana po nazwie;
`prefix` (opcjonalny) zawęża do jednego pakietu. Do podpowiedzi / drzewa w formularzu (drzewo frontend buduje, dzieląc
nazwy po kropce).

```json
[
  { "name": "net.focik.homeoffice.goahead", "configuredLevel": null, "effectiveLevel": "INFO" },
  { "name": "net.focik.homeoffice.goahead.domain.invoice.KsefService", "configuredLevel": "DEBUG", "effectiveLevel": "DEBUG" }
]
```

`configuredLevel = null` — logger dziedziczy poziom. Lista zawiera tylko loggery już utworzone (klasa pojawia się
dopiero po pierwszym załadowaniu), więc pole tekstowe z ręczną nazwą nadal ma sens jako fallback.

### `PUT /logs/levels`

Body: `{ "logger": "net.focik.homeoffice.finance", "level": "DEBUG", "ttlMinutes": 30 }` (`ttlMinutes` opcjonalne).
Odpowiedź `200`: obiekt jak element `overrides` powyżej.

### `DELETE /logs/levels/{logger}`

`204` bez treści; `404`, gdy dla loggera nie ma aktywnego nadpisania.

### Błędy

Format (wspólny dla API, `ExceptionHandling`):

```json
{ "httpStatusCode": 400, "timestamp": "09-25-2026 10:00:00", "httpStatus": "BAD_REQUEST", "reason": "...", "message": "..." }
```

| Status | Kiedy |
|---|---|
| `400` | walidacja w serwisach (`ObjectNotValidException`): `from ≥ to`, zakres > 7 dni, logger spoza dozwolonych prefiksów, brak `level`, TTL poza zakresem, ujemny `after` |
| `401` / `403` | brak/nieważny JWT albo brak uprawnień |
| `404` | `DELETE /levels/{logger}` bez aktywnego nadpisania |
| `500` | błąd odczytu z S3/katalogu logów (`LogsReadException`) — treść błędu w `message` |
| `500` (nieprzetestowane) | błędy bindowania żądania: nieznany poziom w `levels`/`level`, zły format daty, brak wymaganego parametru albo niepoprawne JSON-owe body. `ExceptionHandling` nie ma dla nich osobnych handlerów (jest tylko ogólny `RuntimeException`), więc wg kodu dają `500`, a nie `400` — frontend powinien walidować te dane przed wysłaniem |

## Konfiguracja

| Klucz / zmienna | Domyślnie | Opis |
|---|---|---|
| `APP_INSTANCE` | `unknown` (`local` w `docker-compose.yml`) | nazwa wdrożenia w kluczach S3 i we wpisach; `ec2`, `synology` ustawiane w workflow |
| `aws.bucket-name` (`BUCKET_NAME`) | `focik-home` | bucket, z którego adapter S3 czyta logi |
| `logs.live.buffer-size` | `2000` | pojemność bufora podglądu na żywo |
| `logs.levels.allowed-prefixes` | `net.focik.homeoffice,org.springframework.security,org.hibernate.SQL,software.amazon.awssdk` | prefiksy loggerów, których poziom można zmieniać (lista po przecinku) |
| `logs.levels.default-ttl-minutes` | `15` | domyślny TTL zmiany poziomu |
| `logs.levels.max-ttl-minutes` | `1440` | maksymalny TTL zmiany poziomu |
| `logging.file.name`, `logging.file.path` | zależnie od profilu | lokalny plik logów i katalog (używany też przez adapter plikowy na `dev`) |

Stałe zaszyte w kodzie (zmiana wymaga zmiany kodu): `limit` 1000/5000 i zakres 7 dni (`LogsService`), limit live
200/2000 (`LiveLogsService`), margines 6 min w `S3LogsRepositoryAdapter` (**zależy od `flushIntervalSeconds=300` w
`logback-spring.xml`** — po zmianie interwału flusha trzeba dostosować margines).

## AWS

### Uprawnienia IAM

Aplikacja używa jednych kluczy do plików i logów. Do logów potrzebuje (dodatkowo do istniejących uprawnień S3):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    { "Effect": "Allow", "Action": "s3:ListBucket", "Resource": "arn:aws:s3:::focik-home",
      "Condition": { "StringLike": { "s3:prefix": ["logs/*"] } } },
    { "Effect": "Allow", "Action": ["s3:PutObject", "s3:GetObject"], "Resource": "arn:aws:s3:::focik-home/logs/*" }
  ]
}
```

Bez `s3:ListBucket` `GET /logs/date` zwraca `500`.

### Retencja (lifecycle) — 90 dni

Retencja jest regułą bucketu, nie kodem aplikacji:

```bash
aws s3api put-bucket-lifecycle-configuration --bucket focik-home --lifecycle-configuration \
  '{"Rules":[{"ID":"expire-app-logs-90d","Status":"Enabled","Filter":{"Prefix":"logs/"},"Expiration":{"Days":90}}]}'
```

**Uwaga:** to polecenie **nadpisuje całą konfigurację lifecycle** bucketu — najpierw sprawdź istniejące reguły
(`aws s3api get-bucket-lifecycle-configuration --bucket focik-home`) i dołącz je do JSON-a.

## Wdrożenie i instancje

Instancje (EC2, Synology, lokalny Docker) piszą do **jednego bucketu i prefiksu `logs/`**, a od siebie odróżnia je
`APP_INSTANCE`:

| Wdrożenie | Skąd `APP_INSTANCE` |
|---|---|
| EC2 | `-e APP_INSTANCE=ec2` w `.github/workflows/ec2.yml` |
| Synology | `-e APP_INSTANCE=synology` w `.github/workflows/deploy-to-synology.yml` (razem z `AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY`, bez których appender nie wyśle logów do S3) |
| lokalny Docker | `docker-compose.yml`: `APP_INSTANCE=${APP_INSTANCE:-local}` |

Lokalny kontener działa na profilu `prod` (domyślny w `application.properties`), więc jego logi trafiają do
produkcyjnego bucketu jako instancja `local` — celowo. Uruchomienie z IDE na profilu `dev` **nie** wysyła logów do S3.

## Struktura kodu

```
logservice/
├── api/             LogsController (historia + live), LogLevelsController, dto/SetLogLevelRequest
├── domain/          LogsService, LiveLogsService, LogLevelsService        (implementują *UseCase bezpośrednio)
│   ├── model/       LogEntry, LogLevel, LogQuery, LogResult, LiveLogsResult, LogLevelOverride, LogLevelsInfo, LoggerInfo
│   ├── exceptions/  LogsReadException
│   └── port/        primary: GetLogsUseCase, GetLiveLogsUseCase, ManageLogLevelsUseCase
│                    secondary: LogsRepository, LiveLogSource, LogLevelControl
└── infrastructure/  LogParser
                     s3/S3LogsRepositoryAdapter  (!dev)   file/FileLogsRepositoryAdapter (dev)
                     live/LogbackLiveLogAdapter           level/SpringLogLevelControl
config/S3LogAppender.java, resources/logback-spring.xml
```

## Testy

`LogParserTest`, `LogsServiceTest`, `S3LogsRepositoryAdapterTest` (mock `S3Client`), `S3LogAppenderTest`,
`LiveLogsServiceTest`, `LogbackLiveLogAdapterTest`, `LogLevelsServiceTest`, `LogLevelsServiceWiringTest`
(`ApplicationContextRunner`), `SpringLogLevelControlTest`. Uruchomienie: `./mvnw test -Dtest='Log*Test,S3Log*Test,LiveLogs*Test'`.
Testy nie łączą się z prawdziwym S3.

## Rozwiązywanie problemów

| Objaw | Przyczyna / co sprawdzić |
|---|---|
| `/logs/date` zwraca `entries: []` | zakres bez danych w S3 (retencja, `to` wyłączne, opóźnienie flusha); filtr `instance`; na `dev` — czy pliki są w `logging.file.path` |
| `/logs/date` zwraca `500` | zwykle brak `s3:ListBucket`/`s3:GetObject` na `logs/*`; szczegóły w `message` odpowiedzi i w logu aplikacji |
| brak najnowszych wpisów w `/logs` | flush S3 co ≤ 5 min — najświeższe wpisy są w `/logs/live` |
| brak logów z Synology w S3 | brak kluczy AWS w kontenerze (workflow) albo błąd wysyłki — szukaj `Failed to upload logs to S3` w statusach logbacka/konsoli |
| `gap: true` w `/logs/live` | bufor (2000) się przewinął między odpytaniami albo aplikacja się zrestartowała — odpytywać częściej / zwiększyć `logs.live.buffer-size` |
| po zmianie poziomu na `DEBUG` wszystko „zalane” | zawęzić do konkretnego pakietu, skrócić TTL, `DELETE /logs/levels/{logger}` |
| `400` przy `PUT /levels` | logger spoza `allowedLoggers` (w tym `ROOT`) albo TTL poza zakresem |
| `500` przy `PUT /levels` / `GET /logs*` mimo poprawnych uprawnień | prawdopodobnie błąd bindowania (nieznany poziom, zły format daty) — patrz tabela błędów |

## Znane ograniczenia i punkty do weryfikacji

- **Live i poziomy dotyczą jednej instancji** (tej, do której frontend jest połączony); historię pozostałych daje
  S3 z opóźnieniem. Ewentualne przełączanie instancji w live wymagałoby adresów instancji po stronie frontendu albo
  proxy między backendami.
- **Odczyt z S3 jest sekwencyjny** i skaluje się liczbą obiektów w zakresie (limit 7 dni i `limit` wpisów ogranicza
  koszt).
- **Wolumen logów na EC2:** workflow montuje `/opt/homeoffice/logs` do `/logs`, a profil `prod` (wg
  `application-prod.properties`) pisze do `/app/logs` — do potwierdzenia, czy montowany wolumen jest w ogóle używany.
  Nie wpływa to na historię w S3.
- `application-dev.properties` i `application-prod.properties` są nieśledzone w repozytorium (lokalne), więc ich
  zawartość nie jest częścią commitów.
- Logi zawierają m.in. adresy URL i komunikaty błędów; poziom `DEBUG` może dodać dane wrażliwe — stąd `ROLE_ADMIN`,
  TTL i lista dozwolonych prefiksów. Dostęp do historii i live mają te same role (`LOGS_READ*`).
- Pole `processId` zostało usunięte z odpowiedzi (wzorzec logbacka nie zapisuje PID-a), a odpowiedź historii jest
  kopertą `{entries, truncated}` zamiast listy (zmiana kontraktu w wersji 6.0.0).
