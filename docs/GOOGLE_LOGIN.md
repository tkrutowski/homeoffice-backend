# Logowanie przez Google

Dokumentacja logowania "Zaloguj się przez Google" (Faza 1 szerszego planu; Faza 2 to
passkeys/WebAuthn — patrz sekcja "Co dalej" na końcu).

## Zasada działania — najważniejsze

**To logowanie NIGDY nie zakłada nowego konta.** Konta w tej aplikacji zakłada się wyłącznie
ręcznie (przez admina). Google służy tu tylko jako dodatkowa metoda **uwierzytelnienia już
istniejącego** konta, dopasowywanego po e-mailu. Jeśli e-mail z konta Google nie pasuje do
żadnego istniejącego użytkownika — logowanie jest odrzucane, żaden rekord nie powstaje.

## Przepływ

1. Frontend/appka mobilna woła **Google Identity Services** (JS SDK w przeglądarce / natywny
   Google Sign-In SDK na mobile) — dzieje się to w całości po stronie klienta.
2. Google zwraca podpisany **ID token** (JWT) bezpośrednio do klienta.
3. Klient wysyła `POST /api/v1/auth/google { "idToken": "..." }` do backendu — zwykłe
   wywołanie REST, bez żadnego przekierowania.
4. Backend (`GoogleAuthService`):
   - weryfikuje podpis/`aud`/`exp` tokena przez `GoogleIdTokenVerifier` (biblioteka
     `google-api-client`, klucze publiczne pobierane z JWKS Google — bez kontaktu z sekretem),
   - sprawdza `email_verified == true` w payloadzie,
   - szuka `AppUser` po e-mailu (`GetUserUseCase.findUserByEmail`) — **nie tworzy nowego**,
   - jeśli brak konta → `UserNotFoundException` (HTTP 400), zero side-effectów,
   - jeśli konto istnieje: ręcznie sprawdza `isEnabled()` / `isAccountNonLocked()` (te
     kontrole normalnie robi `AuthenticationManager`, ale on jest tu pomijany),
   - przy pierwszym udanym logowaniu zapisuje `sub` z tokena Google w nowej kolumnie
     `users.google_sub` (trwałe powiązanie konta),
   - przy kolejnych logowaniach porównuje zapisany `google_sub` z przychodzącym — przy
     niezgodności odrzuca logowanie zamiast cicho nadpisywać powiązanie,
   - wystawia **te same JWT** co przy logowaniu hasłem (`JwtService.generateToken` /
     `generateRefreshToken`) — z perspektywy reszty aplikacji (autoryzacja, refresh, wylogowanie)
     nie ma żadnej różnicy między kontem zalogowanym hasłem a przez Google.

Backend **nigdy nie jest celem przekierowania z Google** i nigdy nie używa Client Secret —
to nie jest klasyczny server-side OAuth2 "Authorization Code" flow
(`spring-boot-starter-oauth2-client`), tylko weryfikacja ID tokena po stronie API. To
świadomy wybór architektoniczny, dopasowany do stateless REST API konsumowanego przez SPA
i appkę mobilną (żadna redirect-based sesja po stronie backendu).

## Kontrakt API

```
POST /api/v1/auth/google
Content-Type: application/json

{ "idToken": "<JWT zwrocony przez Google>" }
```

| Sytuacja | HTTP | Body |
|---|---|---|
| Sukces | 200 | `{ "accessToken": "...", "refreshToken": "..." }` (jak `/api/v1/auth/login`) |
| Brak konta o tym e-mailu | 400 | komunikat: "Brak konta powiazanego z tym adresem e-mail..." |
| Nieprawidłowy/wygasły token, konto zablokowane/wyłączone, niezgodność `google_sub`, `email_verified=false` | 401 | ogólny komunikat błędu autentykacji |

Endpoint jest publiczny (`cors.public-url`), tak jak `/api/v1/auth/login` i `/refresh`.

## Model danych

`AppUser.googleSub` → kolumna `users.google_sub` (`VARCHAR(255)`, `NULL`, `UNIQUE`).
Migracja: `src/main/resources/db/migration/V5__add_google_sub_to_users.sql`.

W MySQL wartości `NULL` nie kolidują z ograniczeniem `UNIQUE`, więc konta bez logowania
Google współistnieją bez problemu.

`AppUser.avatarUrl` → kolumna `users.avatar_url` (`VARCHAR(512)`, `NULL`).
Migracja: `src/main/resources/db/migration/V6__add_avatar_url_to_users.sql`. Wypełniane/odświeżane
wyłącznie przy logowaniu przez Google (pole `picture` z ID tokena — bezpośredni URL do CDN
Google, `lh3.googleusercontent.com`, nie pobieramy/nie hostujemy obrazka u siebie). Wystawione
we froncie przez `GET /api/v1/user/me` (`UserDto.avatarUrl`). `NULL` dla userów, którzy nigdy
nie logowali się przez Google — front pokazuje wtedy avatar z inicjałów jak dziś.

## Konfiguracja

Property `google.oauth.client-ids` (lista Client ID dozwolonych jako `audience` tokena,
rozdzielona przecinkami — osobne dla web/Android/iOS, jeśli kiedyś dojdą) — **wymagane, bez
wartości domyślnej**, tak jak `JWT_SECRET_KEY`. Aplikacja nie wystartuje bez niej.

```properties
# application.properties
google.oauth.client-ids=${GOOGLE_OAUTH_CLIENT_IDS}
```

Gdzie ustawić wartość:
- **Lokalnie / docker-compose**: `.env` (nieśledzony przez git) — klucz `GOOGLE_OAUTH_CLIENT_IDS`.
- **Profil dev z IDE**: `src/main/resources/application-dev.properties` (nieśledzony), wprost
  jako `google.oauth.client-ids=...`.
- **Produkcja**: zmienna środowiskowa `GOOGLE_OAUTH_CLIENT_IDS` tam, gdzie dziś ustawiane są
  `DB_URL`, `JWT_SECRET_KEY` itd.

Client ID **nie jest sekretem** (trafia jawnie do kodu frontendu/appki) — bezpiecznie można go
wpisać wprost w repo frontendu. **Client Secret nie jest używany w ogóle** — w tym flow
(publiczny klient, Google Identity Services) backend nie wymienia kodu na token przez endpoint
tokenowy Google, więc nie ma czego uwierzytelniać sekretem.

## Konfiguracja w Google Cloud Console

1. Projekt → **OAuth consent screen**.
2. **Credentials → Create Credentials → OAuth client ID**, typ **Web application**.
3. **Authorized JavaScript origins** — domeny frontu (krytyczne, bez tego GIS nie zainicjuje
   się w przeglądarce): `https://focikhome.netlify.app`, `https://goahead.netlify.app`,
   `http://localhost:5173` (dev).
4. **Authorized redirect URIs** — w tym flow **nieużywane funkcjonalnie** (nie ma
   server-side redirectu). Można wpisać te same domeny frontu jako nieszkodliwy placeholder,
   jeśli konsola wymusza niepuste pole.
5. Skopiowany **Client ID** → `GOOGLE_OAUTH_CLIENT_IDS` (patrz wyżej). Client Secret ignorować.

Dla appki mobilnej: osobne Client ID typu **Android** / **iOS** (też bez sekretu) — dopisywane
do tej samej, rozdzielonej przecinkami wartości `GOOGLE_OAUTH_CLIENT_IDS`.

## Zmienione/dodane pliki (backend)

| Plik | Rola |
|---|---|
| `userservice/api/dto/GoogleLoginRequest.java` | DTO żądania (`idToken`) |
| `userservice/application/GoogleAuthService.java` | logika: weryfikacja tokena, dopasowanie po e-mailu, wystawienie JWT |
| `userservice/domain/security/config/GoogleAuthConfig.java` | bean `GoogleIdTokenVerifier` z listą dozwolonych `audience` |
| `userservice/api/AuthController.java` | nowy endpoint `POST /api/v1/auth/google` |
| `userservice/domain/AppUser.java` | pola `googleSub`, `avatarUrl` |
| `userservice/domain/port/primary/GetUserUseCase.java` | nowa metoda `findUserByEmail` |
| `userservice/application/UserAppService.java` | implementacja `findUserByEmail` (delegacja do `UserFacade`) |
| `userservice/api/dto/UserDto.java` | pole `avatarUrl` (wystawiane przez `GET /api/v1/user/me`) |
| `db/migration/V5__add_google_sub_to_users.sql` | kolumna `google_sub` |
| `db/migration/V6__add_avatar_url_to_users.sql` | kolumna `avatar_url` |
| `application.properties` | `google.oauth.client-ids`, dopisany publiczny URL |
| `pom.xml` | zależność `com.google.api-client:google-api-client` |

## Kluczowe decyzje bezpieczeństwa

- **Brak auto-rejestracji** — jedyna metoda zakładania kont to ręczna rejestracja przez admina;
  Google login odrzuca nieznane e-maile zamiast tworzyć konto.
- **`email_verified` wymagane** — bez tego dopasowanie po e-mailu byłoby podatne na przejęcie
  konta przez kogoś z niezweryfikowanym adresem u dostawcy tożsamości.
- **Ręczne powtórzenie kontroli `isEnabled`/`isAccountNonLocked`** — bo `AuthenticationManager`
  (który normalnie to robi) jest w tym flow pomijany.
- **Guard na niezgodność `google_sub`** — po pierwszym powiązaniu konto-Google, kolejne
  logowania muszą zgadzać się co do `sub`; przy niezgodności logowanie jest odrzucane, nie
  cicho nadpisywane.
- **Brak Client Secret gdziekolwiek w kodzie** — architektura (publiczny klient, ID-token flow)
  go nie wymaga.

## Frontend

Wymagana integracja: Google Identity Services (`https://accounts.google.com/gsi/client`) →
przycisk logowania → callback z `idToken` → `POST /api/v1/auth/google` → zapis
`accessToken`/`refreshToken` **tym samym mechanizmem co przy zwykłym loginie** (bez osobnej
ścieżki auth state). Szczegółowy prompt do wdrożenia po stronie frontu — patrz historia
konwersacji z 2026-09-23 (sesja backendowa) albo poproś o przygotowanie ponownie.

## Co dalej

- **Faza 2 (nie zaimplementowana)**: logowanie przez **passkeys/WebAuthn** (odcisk palca/Face ID
  na telefonie, Windows Hello na desktopie) — wymaga osobnego `SecurityFilterChain` z sesją
  tylko dla ścieżek `/webauthn/**` (główny łańcuch API zostaje `STATELESS`), tabel z modułu
  `spring-security-webauthn` i integracji z `JwtService` analogicznej do powyższej. Rejestracja
  klucza dostępna tylko dla już zalogowanego użytkownika (nie jako sposób zakładania konta).
- **Faza 3**: passkeys w appce mobilnej — wymaga hostowania `assetlinks.json` (Android) /
  `apple-app-site-association` (iOS) na domenie produkcyjnej, skoordynowane z buildem appki.
