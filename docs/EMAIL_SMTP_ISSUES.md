# Problemy z wysyłką maili (SMTP) i sekretami w konfiguracji

Dokument powstał po analizie błędu z logów (2026-09-14 09:30, wysyłka `expense-report.html` do
`tkrutowski@gmail.com`). Zebrano tu główny problem oraz dodatkowe usterki znalezione przy okazji
przeglądu `EmailServiceAdapter` i konfiguracji SMTP — w tym jedno znalezisko wykraczające poza
temat maili (punkt 4: realne sekrety jako wartości domyślne w niezacommitowanych lokalnych
zmianach `application.properties` — **nie w gicie**, ale ryzyko przyszłego przypadkowego
commita).

**Punkty 1-3 naprawione** (patrz opis zmian pod każdym). **Punkt 4 nadal czeka** — to zmiana
organizacyjna (jak trzymać lokalne sekrety), nie fix w kodzie.

## 1. ✅ Timeout przy autoryzacji SMTP (`smtp.webio.pl`) — naprawione

**Priorytet: średni** (objawia się utratą maili, ale bez wpływu na resztę aplikacji)

### Objawy

```
MailAuthenticationException: Authentication failed
  Caused by: jakarta.mail.AuthenticationFailedException
    Caused by: MessagingException: Exception reading response
      Caused by: SocketTimeoutException: Read timed out
```

Wystąpiło dwukrotnie pod rząd (wątki `email-1`, `email-2`) dla tego samego maila.

### Przyczyna

To **nie jest błąd złych danych logowania** — mimo etykiety `AuthenticationFailedException`,
prawdziwą przyczyną (najgłębszy `Caused by`) jest `SocketTimeoutException: Read timed out`.
Połączenie TCP + `STARTTLS` zostało nawiązane poprawnie, aplikacja wysłała komendę `AUTH`, ale
serwer `smtp.webio.pl:587` nie odpowiedział w ciągu skonfigurowanych 5 sekund
(`spring.mail.properties.mail.smtp.timeout=5000`).

Konfiguracja: `src/main/resources/application.properties:96-105`
```properties
spring.mail.host=smtp.webio.pl
spring.mail.port=587
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000
```

Może to być chwilowa niedostępność/przeciążenie serwera pocztowego albo problem sieciowy między
hostem aplikacji a `webio.pl`. Jeśli powtarza się częściej niż sporadycznie, warto to
monitorować (np. licząc błędy `MailException` w logach) zanim zdecydujemy się na któryś z
poniższych fixów.

### Co zostało zrobione

1. **Zwiększone timeouty** — `application.properties:103-105`: `connectiontimeout`/`timeout`/
   `writetimeout` z 5000 na **15000** ms.
2. **Dodany retry z backoffem** — `EmailServiceAdapter.doSendSimpleEmail`/`doSendHtmlEmail`:
   do 3 prób wysyłki, 2s odstępu między nimi (pole `retryBackoffMs`, ustawiane na `0` w testach,
   żeby ich nie spowalniać). Po wyczerpaniu prób błąd jest logowany (`ERROR`) i połykany — wysyłka
   maila nadal nie blokuje wątku wywołującego.
3. **Throttling ze strony `webio.pl`** — nadal niesprawdzone, zostawione jako coś do obserwacji,
   gdyby problem wracał mimo dłuższych timeoutów i retry.

Testy: `EmailServiceAdapterTest` (6 przypadków — sukces za pierwszym razem, retry-i-sukces,
wyczerpanie prób bez wyjątku, dla obu `sendSimpleEmail`/`sendHtmlEmail` oraz `sendTemplatedEmail`).

## 2. ✅ Mylący log w `sendTemplatedEmail` — naprawione

**Priorytet: niski** (tylko log, nie wpływa na działanie)

`EmailServiceAdapter.java:100-101`:
```java
sendHtmlEmail(request.getTo(), request.getSubject(), htmlContent);
log.debug("Templated email processed and sent from template: {}", request.getTemplateName());
```

`sendHtmlEmail` łapie **wszystkie** wyjątki wewnątrz siebie (`MessagingException`,
`MailException`, `Exception`) i tylko je loguje — nic nie rzuca dalej. W efekcie
`sendTemplatedEmail` zawsze dochodzi do linii `log.debug(...)`, niezależnie od tego, czy mail
faktycznie wyszedł. Dokładnie to widać w logu z błędu: zaraz po `ERROR ... Mail exception...`
pojawia się `DEBUG ... Templated email processed and sent`.

### Co zostało zrobione

Logika wysyłki HTML maila (z retry) wydzielona do prywatnej `doSendHtmlEmail(...)`, zwracającej
`boolean`. `sendTemplatedEmail` woła ją bezpośrednio (nie przez `sendHtmlEmail`) i loguje
`DEBUG "...sent..."` tylko gdy naprawdę się udało, w przeciwnym razie `WARN "...NOT sent..."`.
Przy okazji rozwiązuje to punkt 3 (self-invocation) — patrz niżej.

## 3. ✅ `@Async` na `sendHtmlEmail` nie działało, gdy wywoływane z `sendTemplatedEmail` — naprawione

**Priorytet: niski/informacyjny** (dziś nieszkodliwe, ale warto wiedzieć)

`sendTemplatedEmail` (już `@Async`) wywołuje `sendHtmlEmail(...)` jako zwykłe wywołanie metody
na `this` (`EmailServiceAdapter.java:100`). To klasyczny problem Spring AOP: **self-invocation
pomija proxy**, więc adnotacja `@Async` na `sendHtmlEmail` jest w tej ścieżce wywołania
ignorowana — `sendHtmlEmail` wykonuje się synchronicznie w tym samym wątku co
`sendTemplatedEmail` (widać to też w stack trace błędu — obie metody są w jednej ramce
wywołań, bez przejścia przez `AsyncExecutionInterceptor` drugi raz).

Obecnie nie robi to różnicy funkcjonalnej (i tak całość leci w wątku `emailTaskExecutor` przez
zewnętrzne `@Async` na `sendTemplatedEmail`), ale jest mylące i warto to uporządkować przy
okazji naprawy punktu 2.

### Co zostało zrobione

Usunięto wewnętrzne wywołanie `this.sendHtmlEmail(...)` z `sendTemplatedEmail` — obie metody
(`sendHtmlEmail` publiczne, `@Async`, wołane też bezpośrednio przez `EmailController`, oraz
`sendTemplatedEmail`) teraz wołają wspólną prywatną `doSendHtmlEmail(...)`, więc self-invocation
w ogóle nie występuje. `@Async` na `sendHtmlEmail` nadal działa poprawnie dla zewnętrznych
wywołań (np. `EmailController.java:70`), bo te nadal idą przez proxy Springa.

## 4. Prawdziwe hasła/sekrety jako wartości domyślne w lokalnych (niezacommitowanych) zmianach `application.properties`

**Priorytet: średni (bezpieczeństwo — ryzyko, nie aktualny wyciek)**

**Sprostowanie:** pierwsza wersja tego dokumentu błędnie sugerowała, że te sekrety trafiły do
gita. Nie trafiły — sprawdzone przez `git show HEAD:...` i `git diff HEAD`: w ostatnim commicie
(`HEAD`) te linie wyglądają tak (bez żadnej wartości domyślnej):
```properties
security.jwt.secret-key=${JWT_SECRET_KEY}
spring.datasource.password=${DB_PASSWORD}
aws.secret-key=${AWS_SECRET_ACCESS_KEY}
spring.mail.password=${MAIL_PASSWORD}
```
Realne wartości po dwukropku (`${JWT_SECRET_KEY:404E63...}` itd.) istnieją **tylko lokalnie, w
niezacommitowanych zmianach w working tree** (`git status` pokazuje `application.properties` jako
zmodyfikowany, nie dodany do commita) — prawdopodobnie dodane dla wygody lokalnego uruchamiania
bez ustawiania zmiennych środowiskowych za każdym razem. **Nic z tego nie jest w historii gita i
nie zostało wypchnięte.**

Ryzyko jest więc inne niż "wyciek" — to ryzyko **przyszłego przypadkowego commita** (`git add .`
/ `git commit -a` bez przejrzenia diffa) tego pliku z prawdziwymi sekretami w środku. Dodatkowo
`../src/main/resources/application-dev.properties` i `application-prod.properties` są dziś
**untracked** (nowe pliki, jeszcze nigdy niecommitowane) i **nie są objęte `.gitignore`** — więc
też mogą trafić do repo przy nieuważnym `git add .`, jeśli zawierają realne dane (nie
sprawdzałem ich zawartości w ramach tego przeglądu).

### Jak naprawić

1. Nie zostawiać prawdziwych sekretów jako wartości domyślnych nawet lokalnie — zamiast
   `${JWT_SECRET_KEY:404E63...}` w wersjonowanym pliku, trzymać lokalne wartości w mechanizmie,
   który i tak jest już w `../.gitignore` (`.env`, `.env.local` — sekcja `### Env ###` w
   `.gitignore` już to pokrywa) i ładować je stamtąd, albo po prostu eksportować jako zmienne
   środowiskowe przed uruchomieniem (`SPRING_PROFILES_ACTIVE=dev` + reszta already opisane w
   CLAUDE.md pod "Required env vars").
2. Przed każdym `git add`/`git commit` obejmującym `application.properties` — świadomie sprawdzić
   `git diff --staged`, żeby nie wynieść tych wartości do historii przez pomyłkę.
3. Dodać `application-dev.properties` i `application-prod.properties` do `../.gitignore` (albo
   upewnić się, że nie zawierają sekretów, zanim się je kiedyś doda do repo) — dziś nic nie
   chroni przed ich przypadkowym scommitowaniem.
4. Rozważyć git hook (`pre-commit`) albo narzędzie typu `gitleaks`/`git-secrets`, które blokuje
   commit zawierający wzorce wyglądające jak sekret — tańsze niż każdorazowe ręczne sprawdzanie.
