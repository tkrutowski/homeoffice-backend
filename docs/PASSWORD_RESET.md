# Reset hasła ("zapomniałem hasła")

Dokumentacja samoobsługowego odzyskiwania dostępu do konta przez link mailowy. Niezależne od
innych metod logowania — [`GOOGLE_LOGIN.md`](GOOGLE_LOGIN.md), [`PASSKEYS_LOGIN.md`](PASSKEYS_LOGIN.md)
— to nie jest metoda logowania, tylko sposób ustawienia nowego hasła do istniejącego konta.

## Zasada działania — najważniejsze

- `POST /forgot-password` **zawsze zwraca `200 OK`**, niezależnie czy podany e-mail istnieje w
  bazie — celowa ochrona przed enumeracją kont (nie da się w ten sposób sprawdzić, czy dany
  adres ma konto w aplikacji).
- Token resetu jest **jednorazowy**, ważny **30 minut**, i w bazie trzymany wyłącznie jako
  **hash SHA-256** — token w postaci jawnej istnieje tylko w linku wysłanym mailem, nigdy nie
  trafia do logów ani do bazy danych.
- Reset hasła **nie unieważnia** już wydanych JWT (access/refresh) — API jest stateless, nie ma
  blacklisty tokenów. Istniejące zalogowane sesje żyją dalej do naturalnego wygaśnięcia (do
  24h dla access, 7 dni dla refresh). Świadomy kompromis, patrz sekcja "Kluczowe decyzje
  bezpieczeństwa" niżej.

## Przepływ

1. Użytkownik na stronie logowania klika „Zapomniałem hasła" → formularz z adresem e-mail.
2. Frontend: `POST /api/v1/auth/forgot-password { "email": "..." }` → zawsze `200 OK`.
3. Backend (`PasswordResetFacade.requestPasswordReset`):
   - szuka `AppUser` po e-mailu — jeśli brak, cicho kończy (zero efektów ubocznych, brak wyjątku),
   - jeśli istnieje już aktywny (nieużyty) token utworzony w ciągu ostatnich 2 minut — też cicho
     kończy (prosty anty-spam zamiast pełnego rate limitera),
   - unieważnia (`used_at = now`) wszystkie inne aktywne tokeny tego użytkownika,
   - generuje nowy losowy token (32 bajty `SecureRandom`, base64url), zapisuje jego SHA-256
     hash + `expires_at = now + 30 min`,
   - wysyła mail (szablon `templates/emails/password-reset.html`, przez istniejący
     `EmailNotificationPort.sendTemplatedEmail`, asynchronicznie) z linkiem
     `${app.password-reset.frontend-url}?token=<rawToken>`.
4. Użytkownik klika link w mailu → frontend na stronie resetu odczytuje `token` z query stringa,
   pokazuje formularz nowego hasła.
5. Frontend: `POST /api/v1/auth/reset-password { "token": "...", "newPassword": "..." }`.
6. Backend (`PasswordResetFacade.resetPassword`):
   - szuka tokena po hashu, sprawdza `PasswordResetToken.isValid()` (nieużyty i nie wygasł) →
     w razie błędu `InvalidPasswordResetTokenException` (400),
   - waliduje politykę hasła (`UserConstant.PASSWORD_POLICY` — min. 8 znaków, 1 cyfra, 1 znak
     specjalny; ta sama reguła co przy `PUT /api/v1/user/change-password`) → w razie błędu
     `WeakPasswordException` (400),
   - ustawia nowe (BCrypt) hasło, oznacza token jako użyty.

## Kontrakt API

### `POST /api/v1/auth/forgot-password`

```
Content-Type: application/json

{ "email": "user@example.com" }
```

| Sytuacja | HTTP | Body |
|---|---|---|
| Zawsze (niezależnie czy e-mail istnieje) | 200 | brak treści |

Endpoint publiczny (`cors.public-url`).

### `POST /api/v1/auth/reset-password`

```
Content-Type: application/json

{ "token": "<token z linku>", "newPassword": "NoweHaslo123!" }
```

| Sytuacja | HTTP | `message` |
|---|---|---|
| Sukces | 200 | brak treści |
| Token nieprawidłowy / wygasły / już użyty | 400 | `"Link do resetu hasła jest nieprawidłowy lub wygasł."` |
| Hasło nie spełnia polityki (min. 8 znaków, 1 cyfra, 1 znak specjalny) | 400 | `"Hasło musi mieć co najmniej 8 znaków, jedną cyfrę i jeden znak specjalny."` |

Endpoint publiczny (`cors.public-url`). Format błędu jak w reszcie API — `HttpResponse` z
`ExceptionHandling`, pełny kształt:
```json
{
  "httpStatusCode": 400,
  "timestamp": "09-24-2026 12:00:00",
  "httpStatus": "BAD_REQUEST",
  "reason": "Link do resetu hasła jest nieprawidłowy lub wygasł.",
  "message": "Link do resetu hasła jest nieprawidłowy lub wygasł."
}
```
(`reason` i `message` są dla tych dwóch błędów identyczne — handler przekazuje ten sam tekst w oba pola).

## Model danych

Tabela `password_reset_tokens` (`db/migration/V8__add_password_reset_tokens.sql`):

| Kolumna | Typ | Opis |
|---|---|---|
| `id` | BIGINT | PK |
| `user_id` | BIGINT | FK → `users.id`, `ON DELETE CASCADE` |
| `token_hash` | VARCHAR(64) | SHA-256 hex, `UNIQUE` |
| `expires_at` | DATETIME | `created_at + 30 min` |
| `used_at` | DATETIME NULL | `NULL` = token wciąż aktywny |
| `created_by` / `modified_by` / `created_at` / `modified_at` | — | z `AuditableEntity`, jak pozostałe encje domenowe (`AppUser`, `Role`, `Privilege`) |

## Konfiguracja

```properties
app.password-reset.frontend-url=${PASSWORD_RESET_FRONTEND_URL:https://focikhome.netlify.app/reset-password}
```

`PASSWORD_RESET_FRONTEND_URL` to **strona frontendu**, do której doklejany jest `?token=...`.
Celowo osobna zmienna od `HOME_URL`/`homeoffice.url`, który wskazuje na bucket S3 (niezwiązany
cel — pliki, nie strony). Nadpisz lokalnie w `application-dev.properties`, jeśli frontend dev
chodzi pod innym adresem niż domyślny.

## Kluczowe decyzje bezpieczeństwa

- **Anty-enumeracja**: `/forgot-password` zawsze `200 OK`, niezależnie od wyniku.
- **Token nieodwracalny w bazie**: przechowywany tylko jako SHA-256 hash — wyciek tabeli
  `password_reset_tokens` nie daje gotowych tokenów do użycia.
- **Jednorazowość + wygaśnięcie**: `used_at` + `expires_at`, sprawdzane razem w
  `PasswordResetToken.isValid()`.
- **Unieważnianie starych tokenów**: nowe żądanie resetu unieważnia poprzednie aktywne tokeny
  tego samego użytkownika — nie można mieć wielu ważnych linków naraz.
- **Prosty anty-spam**: nowy token nie powstaje, jeśli poprzedni powstał < 2 minuty temu
  (zamiast pełnego rate limitera per IP/e-mail).
- **Nieunieważnione sesje JWT po resecie** — świadomy kompromis (JWT stateless, brak
  blacklisty). Do rozważenia w przyszłości, jeśli okaże się potrzebne (np. przy podejrzeniu
  przejęcia konta) — wymagałoby dodania blacklisty tokenów, dziś nieistniejącej w projekcie.

## Zmienione/dodane pliki (backend)

| Plik | Rola |
|---|---|
| `userservice/domain/PasswordResetToken.java` | encja tokena |
| `userservice/domain/PasswordResetFacade.java` | logika: generowanie/hash tokena, TTL, jednorazowość, anty-spam, wysyłka maila, reset hasła |
| `userservice/domain/port/primary/RequestPasswordResetUseCase.java` / `ResetPasswordUseCase.java` | porty (primary) |
| `userservice/domain/port/secondary/IPasswordResetTokenRepository.java` | port repozytorium (secondary) |
| `userservice/infrastructure/jpa/IPasswordResetTokenDtoRepository.java` / `PasswordResetTokenRepositoryAdapter.java` | adapter JPA |
| `userservice/api/dto/ForgotPasswordRequest.java` / `ResetPasswordRequest.java` | DTO żądań |
| `userservice/api/AuthController.java` | `POST /forgot-password`, `POST /reset-password` |
| `userservice/domain/exceptions/InvalidPasswordResetTokenException.java` | wyjątek nieprawidłowego/wygasłego tokena |
| `utils/exceptions/ExceptionHandling.java` | mapowanie wyjątku → 400 |
| `userservice/domain/security/constant/UserConstant.java` | wspólna `PASSWORD_POLICY` (wydzielona z `UserServiceImpl`), `INVALID_RESET_TOKEN` |
| `templates/emails/password-reset.html` | szablon maila z linkiem resetującym |
| `db/migration/V8__add_password_reset_tokens.sql` | tabela `password_reset_tokens` |
| `application.properties` | `app.password-reset.frontend-url`, dopisane publiczne URL-e w `cors.public-url` |

## Frontend

Stack: Vue 3 + Vite (`focikhome.netlify.app`). Wymagane dwa nowe ekrany/widoki (formularz
"zapomniałem hasła" i formularz nowego hasła pod tokenem) plus link na stronie logowania.
Szczegółowy prompt do wdrożenia — patrz historia konwersacji z 2026-09-24 (sesja backendowa) w
repo `homeoffice-backend`, albo poproś o przygotowanie ponownie.
