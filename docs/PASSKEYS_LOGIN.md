# Logowanie przez passkeys (WebAuthn)

Dokumentacja Fazy 2 planu logowania (Faza 1 to Google login — [`GOOGLE_LOGIN.md`](GOOGLE_LOGIN.md)).
Passkey = logowanie odciskiem palca / Face ID / Windows Hello zamiast hasła, oparte o
standard WebAuthn/FIDO2, wbudowane w Spring Security 7 (`spring-security-webauthn`).

## Zasada działania — najważniejsze

Tak jak logowanie Google: **rejestracja klucza jest dostępna tylko dla już zalogowanego
użytkownika** (istniejące konto, zalogowane hasłem/Google). To NIE jest sposób zakładania
nowego konta — klucz dowiązuje się do konta, które już istnieje.

## `rpId` — dlaczego akurat `focikhome.netlify.app`

WebAuthn wymaga ustalenia **Relying Party ID (`rpId`)** — domeny, na stałe zaszytej w każdym
zarejestrowanym kluczu (zmiana `rpId` po fakcie psuje już zarejestrowane passkeye, trzeba by
rejestrować od nowa). `rpId` musi dokładnie odpowiadać domenie, z której działa frontend
wykonujący logowanie.

`*.netlify.app` jest na **Public Suffix List** — przeglądarka traktuje każdą subdomenę
(`focikhome.netlify.app`, `goahead.netlify.app`) jako zupełnie osobną, niepowiązaną "stronę"
(dokładnie jak `*.github.io`). Dlatego `rpId` może objąć tylko jedną z nich naraz — świadomie
wybraliśmy **wyłącznie `focikhome.netlify.app`**. `goahead.netlify.app` nie ma i nie będzie mieć
passkeys, dopóki nie stanie pod własną (nie-PSL) domeną, np. subdomeną `focik.net`.

## Architektura

Ceremonia WebAuthn (rejestracja i logowanie) to protokół dwuetapowy: serwer generuje
`challenge`, zapamiętuje go, klient odpowiada podpisanym potwierdzeniem. Domyślnie Spring
trzyma ten stan w `HttpSession` — ale główny `SecurityFilterChain` API (`SecurityConfig`) jest
celowo `STATELESS`. Rozwiązanie: **osobny, węższy `SecurityFilterChain`**
(`WebAuthnSecurityConfig`, `@Order(1)`, oceniany przed głównym `@Order(2)`), dopasowujący
wyłącznie `/webauthn/**` i `/login/webauthn`, z sesją `IF_REQUIRED`. Reszta API nie jest tym
ruszana.

- **Rejestracja** (`/webauthn/register/options`, `/webauthn/register`, `DELETE
  /webauthn/register/{id}`) wymaga bycia zalogowanym — `jwtAuthFilter` jest dołączony i do
  tego łańcucha, więc `Authorization: Bearer <JWT>` działa tu tak samo jak w reszcie API.
- **Logowanie** (`/webauthn/authenticate/options`, `/login/webauthn`) jest publiczne — to
  alternatywa dla `/api/v1/auth/login`, a nie coś wymagające uprzedniego zalogowania.

### Dlaczego jest dodatkowy endpoint `POST /webauthn/token`

Domyślny `POST /login/webauthn` po udanym logowaniu zwraca **własny** format Spring Security
(`{"redirectUrl": "/", "authenticated": true}`), nie nasze JWT — DSL `.webAuthn(...)` nie
udostępnia prostego haka na podmianę tej odpowiedzi (sprawdzone bezpośrednio w bytecode
`WebAuthnConfigurer`/`WebAuthnAuthenticationFilter` — brak settera na
`AuthenticationSuccessHandler` w publicznym API tej wersji).

Rozwiązanie: po `authenticated: true` frontend **od razu** (ta sama sesja/ciasteczko, patrz
niżej) woła `POST /webauthn/token` (`WebAuthnAuthController`). Ten czyta `Authentication`
ustawiony w sesji przez ceremonię (`authentication.getName()` = username, dokładnie tak jak
robi to wewnętrznie `WebAuthnAuthenticationProvider` przy pobieraniu uprawnień z
`UserDetailsService`), znajduje `AppUser` po username i wystawia te same JWT co przy
logowaniu hasłem/Google. Sesja jest od razu unieważniana — od tego momentu autoryzacja idzie
już tylko przez JWT jak wszędzie indziej.

**Konsekwencja dla frontu:** wszystkie wywołania `/webauthn/**` i `/login/webauthn` muszą iść
z `credentials: 'include'` (ciasteczko sesji), inaczej ceremonia się rozjedzie między krokami.

## Endpointy

| Endpoint | Metoda | Auth | Opis |
|---|---|---|---|
| `/webauthn/register` | GET | JWT | **Nasz własny endpoint** (biblioteka go nie ma) — lista zarejestrowanych kluczy usera: `[{id, label, created, lastUsed}]` |
| `/webauthn/register/options` | POST | JWT | Krok 1 rejestracji — zwraca `challenge` do `navigator.credentials.create()` |
| `/webauthn/register` | POST | JWT | Krok 2 rejestracji — zapisuje nowy klucz |
| `/webauthn/register/{id}` | DELETE | JWT (właściciel) | Usuwa zarejestrowany klucz. `{id}` = `PasskeyDto.id` z listy powyżej (base64url `credentialId`) |
| `/webauthn/authenticate/options` | POST | publiczny | Krok 1 logowania — zwraca `challenge` do `navigator.credentials.get()` |
| `/login/webauthn` | POST | publiczny | Krok 2 logowania — weryfikuje podpis, ustawia sesję. Zwraca `{redirectUrl, authenticated}`, **nie JWT** |
| `/webauthn/token` | POST | sesja z powyższego kroku | Wymienia sesję na `{accessToken, refreshToken}` (nasz własny endpoint) |

Bez JWT `/webauthn/register/options` zwraca dziś `400` (nie `401`) — tak działa filtr
biblioteki, gdy nie ma kontekstu zalogowanego użytkownika; front nie powinien zakładać `401`
konkretnie dla tej ścieżki. `GET /webauthn/register` i `DELETE /webauthn/register/{id}`
zwracają standardowe `401` (nasz kod / domyślny handler biblioteki).

`GET /webauthn/register` czyta `PublicKeyCredentialUserEntityRepository.findByUsername` +
`UserCredentialRepository.findByUserId` bezpośrednio (te same beany co
`WebAuthnSecurityConfig` rejestruje dla biblioteki) — jeśli user nigdy nie zarejestrował
klucza, zwraca po prostu pustą listę `[]`, nie 404.

## Model danych

Dwie nowe tabele, wymagane 1:1 (nazwy/typy kolumn) przez wbudowane repozytoria JDBC
(`JdbcPublicKeyCredentialUserEntityRepository`, `JdbcUserCredentialRepository`,
`org.springframework.security.web.webauthn.management`, Spring Security 7.1.1). Biblioteka nie
publikuje gotowego pliku ze schematem — nazwy/typy ustalone przez odczytanie zapytań SQL
zaszytych w bytecode tych klas. Migracja: `db/migration/V7__add_webauthn_tables.sql`.

- **`user_entities`** (`id`, `name`, `display_name`) — `id` to losowy identyfikator WebAuthn
  (NIE nasz `users.id`!), `name` = `users.username` (dopasowanie do konta aplikacji odbywa się
  w warstwie aplikacyjnej, brak FK do `users`).
- **`user_credentials`** (`credential_id`, `user_entity_user_id`, `public_key`,
  `signature_count`, `uv_initialized`, `backup_eligible`, `backup_state`,
  `authenticator_transports`, `public_key_credential_type`, `attestation_object`,
  `attestation_client_data_json`, `created`, `last_used`, `label`) — jeden użytkownik może mieć
  wiele wierszy (wiele zarejestrowanych urządzeń/kluczy).

Bez tych beanów (`JdbcPublicKeyCredentialUserEntityRepository`/`JdbcUserCredentialRepository`,
`WebAuthnSecurityConfig`) biblioteka domyślnie trzyma klucze **tylko w pamięci** —
znikałyby przy każdym restarcie.

## Konfiguracja

```properties
webauthn.rp-id=${WEBAUTHN_RP_ID:focikhome.netlify.app}
webauthn.rp-name=${WEBAUTHN_RP_NAME:HomeOffice}
webauthn.allowed-origins=${WEBAUTHN_ALLOWED_ORIGINS:https://focikhome.netlify.app}
```

Lokalnie (`application-dev.properties`, nieśledzony) nadpisane na:
```properties
webauthn.rp-id=localhost
webauthn.allowed-origins=http://localhost:5173
```
(WebAuthn wymaga HTTPS **albo** `localhost` — stąd osobna wartość na dev.)

## Zmienione/dodane pliki (backend)

| Plik | Rola |
|---|---|
| `userservice/domain/security/config/WebAuthnSecurityConfig.java` | drugi `SecurityFilterChain` (`/webauthn/**`), beany JDBC repo |
| `userservice/domain/security/config/SecurityConfig.java` | `@Order(2)` na głównym łańcuchu (wymagane przy >1 `SecurityFilterChain`) |
| `userservice/api/WebAuthnAuthController.java` | `GET /webauthn/register` (lista kluczy), `POST /webauthn/token` (wymiana sesji na JWT) |
| `userservice/api/dto/PasskeyDto.java` | DTO listy kluczy (`id`, `label`, `created`, `lastUsed`) |
| `db/migration/V7__add_webauthn_tables.sql` | tabele `user_entities`, `user_credentials` |
| `application.properties` / `application-dev.properties` | `webauthn.rp-id`, `rp-name`, `allowed-origins` |
| `pom.xml` | zależność `org.springframework.security:spring-security-webauthn` |

## Zweryfikowane

Uruchomione lokalnie (profil `dev`, prawdziwa baza dev/prod na `webio.pl`, port 8078 żeby nie
kolidować z inną działającą instancją): migracja V7 zaaplikowana czysto, kontekst Springa
wstał bez błędów (oba `SecurityFilterChain`, JDBC repos), smoke-test:
- `POST /webauthn/authenticate/options` (anonimowo) → `200`, poprawny `challenge`, `rpId: "localhost"`
- `POST /webauthn/register/options` (bez JWT) → `400` (poprawnie zablokowane)
- `POST /login/webauthn` (sfałszowany payload) → `401` (brak crasha)
- `GET /api/v1/auth/test` (główny łańcuch, regresja) → `200` — Google login z Fazy 1 nietknięty

Nie testowano jeszcze **pełnej ceremonii** (prawdziwy klucz sprzętowy/platformowy) — to wymaga
przeglądarki i frontendu; do zrobienia przy integracji frontu.

`GET /webauthn/register` (lista kluczy) dopisany później, tylko skompilowany — korzysta z tych
samych beanów co reszta (już zweryfikowanych w runtime), nie odpalano dla niego osobno
pełnego boot testu.

### Poprawka CORS (znalezione podczas integracji frontu)

Pierwsza wersja `WebAuthnSecurityConfig` nie miała `.cors(Customizer.withDefaults())` ani
jawnego `permitAll()` dla `OPTIONS` w `authorizeHttpRequests` — w przeciwieństwie do głównego
`SecurityConfig`, który oba ma. Efekt: preflight `OPTIONS` (np. przed `POST
/webauthn/register` z nagłówkiem `Authorization` z innego originu, np. `localhost:5173`) wpadał
w `anyRequest().authenticated()`, dostawał `401` z `jwtAuthenticationEntryPoint` **zanim**
globalny `CorsFilter` (`CorsConfig.java`, siedzi za `FilterChainProxy`) zdążył dopisać nagłówki
CORS — przeglądarka blokowała to jako błąd CORS, mimo że to w istocie było zwykłe `401`.

Naprawione przez dodanie do `WebAuthnSecurityConfig` dokładnie tego samego wzorca co w
`SecurityConfig`: `.cors(Customizer.withDefaults())` + `.requestMatchers(HttpMethod.OPTIONS,
"/**").permitAll()`. Zweryfikowane na żywo: `OPTIONS /webauthn/register` z
`Origin: http://localhost:5173` → `200` z poprawnymi `Access-Control-Allow-Origin` /
`Access-Control-Allow-Credentials: true`.

**Wniosek na przyszłość:** każdy nowy, osobny `SecurityFilterChain` w tym projekcie musi
powtórzyć `.cors(...)` + `OPTIONS permitAll()` z głównego łańcucha — to nie jest globalne
ustawienie, tylko coś, co trzeba jawnie skonfigurować w każdym łańcuchu osobno.

## Co dalej

**Faza 3 (nie zaimplementowana)**: passkeys w appce mobilnej. Wymaga hostowania
`assetlinks.json` (Android) / `apple-app-site-association` (iOS) na domenie produkcyjnej,
skoordynowane z buildem appki — patrz `GOOGLE_LOGIN.md`, sekcja "Co dalej".
