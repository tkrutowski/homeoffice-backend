# Wyszukiwanie książek i cykli (tekst, głos, pytania o cykle)

Dokumentacja wyszukiwania książek po tytule i autorze oraz asystenta tekstowego, który rozumie zdania typu
„znajdź mi książkę *Rodzinny interes* autora Chmielarz" albo „znajdź następną książkę z cyklu Bezimienny".
Moduł `library`. Prompt dla agenta pracującego nad frontendem:
[`BOOK_SEARCH_FRONTEND_PROMPT.md`](BOOK_SEARCH_FRONTEND_PROMPT.md).

## Zasada działania — najważniejsze

- Backend **nigdy nie wybiera książki za użytkownika** i nie zapisuje niczego w bazie. Zwraca **kandydatów**
  (skrót: tytuł, autorzy, okładka, link, seria) albo odpowiedź o cyklu. Pełne dane wybranej książki frontend pobiera
  istniejącym `GET /api/v1/library/book/url?url=...`.
- **Claude tylko rozumie tekst** (tytuł, autor, intencja, nazwa cyklu). Jego odpowiedź jest używana wyłącznie jako
  fraza wyszukiwania. Adresy książek i dane zawsze pochodzą ze źródeł (Lubimyczytac, BookBeat), nigdy od modelu,
  więc model nie może podać wymyślonego linku. Prompt każe też ignorować polecenia ukryte w tekście użytkownika.
- **Nie zgadujemy autora.** Jeśli użytkownik nie podał autora, pole jest `null`, a model ma zakaz uzupełniania go
  z własnej wiedzy.
- Wynik jest **konserwatywny**: `bestMatch` (kandydat, którego można zaznaczyć bez pytania) pojawia się tylko wtedy,
  gdy wynik jest jednoznaczny. W razie wątpliwości użytkownik wybiera z listy.
- Dyktowanie głosem dzieje się **w przeglądarce** (Web Speech API). Backend widzi tylko tekst.

## Przepływ

```
tekst użytkownika ("znajdź następną z cyklu Bezimienny")
        │
        ▼
 ClaudeBookQueryExtractorAdapter  ──► BookQuery {intent, title, author, series, editionType, ownershipStatus}
        │
        ├─ intent = FIND_BOOK ──► BookSearchService: źródła (Lubimyczytac → BookBeat) → deduplikacja → ranking → bestMatch
        │                         odpowiedź: {candidates, bestMatch}
        │
        └─ intent = SERIES_* ───► SeriesQueryService:
                                   1. cykl z biblioteki użytkownika (Series), dopasowanie odporne na odmianę
                                   2. pierwszy obsługiwany adres z Series.url (lista rozdzielona ";;")
                                   3. wszystkie tomy ze źródła (Lubimyczytac, strony po 20, cache 6 h)
                                   4. nałożenie książek użytkownika (status każdego tomu)
                                   5. wybór wg intencji: wszystkie / nieprzeczytane / następny
                                  odpowiedź: {series}
```

Intencje (`BookQueryIntent`):

| `intent` | Przykład zdania | Wynik |
|---|---|---|
| `FIND_BOOK` (domyślna) | „znajdź mi audiobook Rodzinny interes autora Chmielarz" | kandydaci + `bestMatch` |
| `SERIES_ALL` | „znajdź wszystkie książki z cyklu Bezimienny" | wszystkie tomy ze statusem |
| `SERIES_UNREAD` | „… z cyklu Bezimienny, których nie przeczytałem" | tomy bez statusu `READ` |
| `SERIES_NEXT` | „znajdź następną książkę z cyklu Bezimienny" | `lastRead` + `next` |

Nieznana lub brakująca intencja z odpowiedzi modelu = `FIND_BOOK`.

## Kontrakt API

Wszystkie endpointy w `BookController`, prefiks `/api/v1/library/book`, uprawnienia
`LIBRARY_READ_ALL` lub `LIBRARY_READ` albo `ROLE_ADMIN` (jak `GET /url`).

### `GET /search?title=&author=`

Wyszukiwanie po tytule i (opcjonalnie) autorze, **bez Claude**. Zwraca zwykłą listę (max 10), posortowaną od
najlepszego dopasowania. Nie ma tu `bestMatch`.

```json
[
  {
    "title": "Rodzinny interes",
    "authors": "Wojciech Chmielarz",
    "cover": "https://s.lubimyczytac.pl/upload/books/5133000/5133755/1187730-170x243.jpg",
    "url": "https://lubimyczytac.pl/ksiazka/5133755/rodzinny-interes",
    "source": "lubimyczytac",
    "series": "Bezimienny",
    "bookInSeriesNo": "4"
  }
]
```

Pusty `title` → `400`. Pola `cover`, `series`, `bookInSeriesNo` mogą być `null`.

### `POST /search/text`

Ciało: `{ "text": "znajdź mi książkę rodzinny interes autora chmielarz" }`. Tekst jest przycinany; pusty lub dłuższy
niż **500 znaków** → `400` (bez ciała). Każde wywołanie to jedno zapytanie do modelu AI.

Odpowiedź ma zawsze `intent` i `query` (to, co zrozumieliśmy, żeby UI mogło to pokazać i pozwolić poprawić):

```json
{
  "intent": "FIND_BOOK",
  "query": { "title": "Rodzinny interes", "author": "Chmielarz", "editionType": "AUDIOBOOK",
             "ownershipStatus": null, "series": null },
  "candidates": [ /* BookSearchResult jak wyżej */ ],
  "bestMatch": { /* jeden z candidates albo null */ },
  "series": null
}
```

- `editionType`: `BOOK` | `EBOOK` | `AUDIOBOOK` | `null`. `ownershipStatus`: `HAVE` | `WANT` | `READ_ONLY` | `null`
  (`WANT` = „chcę / planuję zakup"; te same nazwy co w `OwnershipStatus` frontendu). To **podpowiedzi** do formularza
  półki, nie dane do zapisania.
- `FIND_BOOK`: czytaj `candidates` i `bestMatch`; `series` jest `null`. Brak rozpoznanego tytułu to nie błąd:
  `candidates` jest puste, a `query.title` równe `null`.
- Intencje cyklu: czytaj `series`; `candidates` jest puste, a `bestMatch` równe `null`.

#### Sekcja `series`

```json
{
  "intent": "SERIES_NEXT",
  "query": { "title": null, "author": null, "editionType": null, "ownershipStatus": null, "series": "Bezimienny" },
  "candidates": [],
  "bestMatch": null,
  "series": {
    "status": "OK",
    "title": "Bezimienny",
    "url": "https://lubimyczytac.pl/cykl/32783/bezimienny",
    "options": [],
    "volumes": [],
    "lastRead": { "number": "2", "title": "Dług honorowy", "authors": "Wojciech Chmielarz", "cover": "https://…",
                  "url": "https://lubimyczytac.pl/ksiazka/4983200/dlug-honorowy", "status": "READ", "bookId": 12 },
    "next":     { "number": "3", "title": "Zwykła przyzwoitość", "authors": "Wojciech Chmielarz", "cover": "https://…",
                  "url": "https://lubimyczytac.pl/ksiazka/5084872/zwykla-przyzwoitosc", "status": "READING", "bookId": 13 }
  }
}
```

(Przykład skrócony. `number` to tekst, np. `"15.5"`; `volumes` jest wypełnione dla `SERIES_ALL` i `SERIES_UNREAD`,
a `lastRead`/`next` dla `SERIES_NEXT`.)

`series.status` — frontend zamienia każdy na czytelny komunikat:

| Status | Znaczenie |
|---|---|
| `OK` | jest odpowiedź (`volumes` albo `next`) |
| `SERIES_NAME_MISSING` | model nie rozpoznał nazwy cyklu w tekście |
| `SERIES_NOT_FOUND` | żaden cykl w bibliotece nie pasuje do nazwy |
| `AMBIGUOUS_SERIES` | kilka cykli pasuje tak samo; wybór z `options` (tytuły), po wyborze można ponowić pytanie z dokładną nazwą |
| `NO_SUPPORTED_SOURCE` | cykl nie ma adresu z obsługiwanego serwisu (tylko Lubimyczytac); `title` jest ustawiony |
| `SOURCE_UNAVAILABLE` | serwis nie odpowiedział albo zwrócił pustą listę (np. zmienił się HTML) |
| `NOT_ORDERED` | cykl bez numerów tomów (seria wydawnicza), „następnej" nie da się wskazać |
| `NO_NEXT_VOLUME` | przeczytano już ostatni znany tom (`lastRead` ustawiony, `next` = `null`) |

Status tomu (`volumes[].status`, `lastRead.status`, `next.status`):

| Status | Znaczenie |
|---|---|
| `READ` | przeczytany (którekolwiek wydanie: książka, ebook, audiobook) |
| `READING` | aktualnie czytany |
| `ON_SHELF` | jest na półce, ale nieprzeczytany |
| `NOT_IN_LIBRARY` | nie ma go w bibliotece użytkownika |

`volumes[].url` jest `null` dla książki, którą użytkownik ma w bibliotece, a której źródło nie zwróciło.
`bookId` jest ustawione tylko dla książek, które użytkownik ma (to id książki w katalogu).

#### Kody błędów

| Kod | Kiedy |
|---|---|
| `400` | pusty tekst, tekst dłuższy niż 500 znaków (`/search/text`), pusty `title` (`/search`) |
| `503` | model AI niedostępny lub nie udało się zinterpretować tekstu (`BookQueryExtractionException`) |
| `401`/`403` | brak tokenu / uprawnień |
| `500` (wg kodu, nieprzetestowane) | niepoprawne JSON-owe ciało albo brak parametru `title`; `ExceptionHandling` nie ma dla nich osobnych handlerów (jak opisano w `LOGGING.md`), więc frontend powinien wysyłać poprawne dane |

Pytania o cykle nie rzucają błędami: brak cyklu, źródła itd. to `200` ze statusem w `series.status`.

## Wyszukiwanie książek (`FIND_BOOK`)

`BookSearchService` pyta kolejno wszystkie `BookSearchPort` (adaptery w `infrastructure/<technologia>/`, kolejność
z `@Order`: `lubimyczytac` = 1, `bookbeat` = 2). Awaria jednego źródła jest logowana i pomijana.

1. **Deduplikacja** po (znormalizowany tytuł, pierwszy autor). Przy duplikacie wygrywa źródło wcześniejsze w
   kolejności, czyli Lubimyczytac (lepsze dane).
2. **Ranking**: dokładny tytuł = 4 pkt, tytuł zawierający frazę = 2 pkt, nazwisko autora pasuje = +1 pkt. Przy remisie
   zostaje kolejność ze źródła. Wynik obcięty do 10. Ranking jest potrzebny, bo kolejność Lubimyczytac jest rozmyta
   (przy „Rodzinny interes" + Chmielarz właściwa książka była na 6. miejscu).
3. **`bestMatch`** (tylko `POST /search/text`): pierwszy kandydat, jeśli ma dokładny tytuł, pasującego autora (o ile
   podano) i **ściśle** więcej punktów niż drugi. Konsekwencje: „Rodzinny interes" **bez autora** nigdy nie da
   `bestMatch` (są trzy książki o tym tytule), z autorem już tak; literówka w nazwisku też daje `null`, bo lepiej
   pokazać listę niż zgadywać.

Normalizacja (`SearchText`): małe litery, bez znaków diakrytycznych (osobno `ł`), bez interpunkcji przy porównaniu
tytułów.

## Pytania o cykle (`SERIES_*`)

### Znajdowanie cyklu

Cykl jest szukany wśród `Series` **użytkownika** (nie w świecie), bo tylko tam jest adres strony cyklu. Dopasowanie
`SeriesQueryService.matchSeries`:

- Nazwa z tekstu jest rozbijana na słowa (jednoliterowe litery odpadają, cyfry zostają, np. „Part 2").
- **Dokładny tytuł** wygrywa ze wszystkim. Inaczej cykl pasuje, jeśli **każde** słowo z zapytania pasuje do jakiegoś
  słowa tytułu; słowa pasują, gdy są równe albo różnią się tylko końcówką (≥ 5 liter, różnica długości ≤ 3). Dzięki temu
  „Bezimiennym" znajduje „Bezimienny", a „Star" nie łapie „Starship".
- Remis najlepszych wyników → `AMBIGUOUS_SERIES` + `options` (max 5 tytułów).
- Prompt każe modelowi zwracać nazwę cyklu **w mianowniku** bez słów „cykl"/„seria".

### Źródło listy tomów

`Series.url` to lista adresów rozdzielona `;;` (różne serwisy, bywają duplikaty i pusty ogon). Wybierany jest
**pierwszy adres obsługiwany** przez któryś `SeriesCatalogPort`. Obecnie jest jeden adapter:
`LubimyczytacSeriesCatalogAdapter` (`lubimyczytac.pl/cykl/ID/...` i `/seria/ID/...`).
W danych z chwili pisania ok. 71 z 79 cykli (liczone ręcznie) ma adres Lubimyczytac; reszta (np. „Saga o wiedźminie") dostaje
`NO_SUPPORTED_SOURCE`. Rozwiązanie: dopisać do wiersza adres Lubimyczytac.

**Paginacja.** Strona cyklu pokazuje tylko pierwsze 20 książek (np. Jack Reacher ma 46). Adapter pobiera więc
wszystkie strony przez `POST https://lubimyczytac.pl/series/getMoreBooks`:

| Parametr formularza | Wartość |
|---|---|
| `serieId` | id z adresu (`/cykl/1233/...` → `1233`) |
| `viewType` | `cykle` dla `/cykl/`, `serie` dla `/seria/` |
| `phrase` | puste |
| `sortBy` | `default` |
| `page` | 1, 2, 3, ... |

Odpowiedź to JSON: `data.content` (HTML tych samych kart `div.book-card` co w wyszukiwarce), `data.left`
(ile jeszcze zostało); strona za ostatnią to `404`. Kończymy, gdy `left` ≤ 0 (limit bezpieczeństwa: 10 stron).
Między stronami jest przerwa 0,5–1 s, a wynik jest trzymany w pamięci procesu **6 godzin** (po restarcie cache jest pusty).

### Nałożenie biblioteki użytkownika

`FindUserBookUseCase.findUserBooksInSeries(userName, idSeries)` zwraca pozycje użytkownika z dokładnie tego cyklu
(filtrowane po id cyklu, bo zapytanie w repozytorium szuka po fragmencie nazwy i np. „The Frontiers Saga" łapało by też
„... Part 2"). Każdy tom ze źródła dostaje status:

1. dopasowanie książki użytkownika po **tytule** (znormalizowany, bez interpunkcji),
2. gdy tytuł się różni: po **numerze tomu**, ale tylko jeśli ten numer jest w cyklu **unikalny** (zdarzają się
   zdublowane numery, np. polskie i angielskie wydanie),
3. status: `READ` (którekolwiek wydanie) > `READING` > `ON_SHELF`; brak = `NOT_IN_LIBRARY`.

Książki użytkownika, których źródło nie zwróciło, są **dopisywane na koniec** (z `url: null`), żeby nie znikały z
odpowiedzi i żeby liczyły się przy ustalaniu „ostatnio przeczytanego".

### Logika intencji

- `SERIES_ALL`: wszystkie tomy posortowane po numerze (tomy bez numeru na końcu, przy remisie kolejność źródła).
- `SERIES_UNREAD`: te same tomy bez `READ`. Pusta lista przy `OK` oznacza „wszystko przeczytane".
- `SERIES_NEXT`: **najwyższy numer** tomu `READ` = `lastRead` (nie data `readTo`, bo ta myli się przy czytaniu poza
  kolejnością); `next` = pierwszy tom o numerze **wyższym**. Szczegóły:
  - numery bywają **ułamkowe** (`15.5`, `10.1`) i są osobnymi tomami, więc po 15 następny to 15.5,
  - numery bywają **zdublowane** (`11`, `11`) i dziwne (`0`, `00`); przy remisie wygrywa kolejność źródła,
  - gdy nic nie przeczytano: pierwszy tom od numeru **1** (numer 0 to zwykle prequel/dodatek),
  - brak jakichkolwiek numerów (seria wydawnicza, `/seria/`) → `NOT_ORDERED`,
  - ostatni znany tom przeczytany → `NO_NEXT_VOLUME`,
  - jeśli użytkownik właśnie czyta kolejny tom (`READING`), to on jest zwracany jako `next`.

## Źródła i ich pułapki

| Źródło | Stan | Uwagi |
|---|---|---|
| **Lubimyczytac** | wyszukiwanie + listy cykli | rozumie frazę „tytuł autor"; strony cykli paginowane (patrz wyżej) |
| **BookBeat** | wyszukiwanie | API **nie łączy** tytułu z autorem (zapytanie „tytuł autor" = 0 wyników), więc szukamy samym tytułem, a autora porównuje serwis; zwraca okładkę i serię |
| **Upolujebooka.pl** | brak | za wyzwaniem **Cloudflare** (nawet `/` daje 403 zwykłemu klientowi HTTP); wyszukiwarka to `POST /szukaj.html` z tokenem CSRF |
| **Legimi** | brak | tylko istniejące scrapery URL-i |

Link z BookBeat (`bookbeat.com/pl/book/...`) nie ma dedykowanego scrapera: `GET /book/url` obsłuży go przez
`ClaudeBookScraperAdapter` (wolniej i z kosztem wywołania Claude).

Dostępność audiobooków (Legimi/BookBeat/Storytel/Audioteka, `AudiobookChecker`) to osobny krok na już znanej książce
i nie ma związku z tym modułem.

## Limity, blokady, wydajność

- **Opóźnienia przeciw blokadom** (serwisy blokują klientów, którzy wołają zbyt szybko; użytkownik został kiedyś
  zablokowany przez Upolujebooka): `BookScraperService.findBooksInSeries` czeka 1,5–2,5 s między stronami książek,
  adapter cykli 0,5–1 s między stronami API.
- Źródła w `FIND_BOOK` są pytane **sekwencyjnie**. Lubimyczytac ma timeout 10 s na żądanie (do dwóch żądań: fraza
  „tytuł autor", potem sam tytuł). **BookBeat używa wspólnego `RestTemplate` (`RestTemplateConfig`) bez ustawionych
  timeoutów**, więc zawieszone źródło może wydłużyć całe wyszukiwanie (nie mierzyłem tego); istniejący
  `AudiobookAvailabilityService` chroni się przed tym przez `completeOnTimeout`, a `BookSearchService` jeszcze nie.
- Pierwsze pobranie długiego cyklu to kilka żądań (Jack Reacher: 3 strony, ok. 2,4 s w pomiarze); kolejne w ciągu
  6 godzin z cache (ok. 1 ms). Czasu wywołania Claude nie mierzyłem. Klient HTTP frontendu ma timeout 45 s.

## Konfiguracja

- Klucz Anthropic: `CLAUDE_API_KEY` (`spring.ai.anthropic.api-key`), model `claude-haiku-4-5`, `max-tokens=4096`
  (`application.properties`). Ten sam klucz obsługuje pozostałe funkcje Claude w aplikacji. Bez poprawnego klucza
  wywołanie modelu się nie powiedzie, więc `/search/text` odpowie `503` (nie sprawdzone empirycznie); `GET /search`
  działa bez Claude.
- Brak nowych zmiennych środowiskowych, migracji ani zmian w bazie. Metryki wywołań Claude (`gen_ai_client_*`)
  publikuje Spring AI automatycznie, tak jak przy `ClaudeBookScraperAdapter`.

## Kluczowe decyzje

- **Kandydaci, nie automat.** OCR/rozpoznawanie mowy się mylą, a tytuły bywają niejednoznaczne, więc zawsze lista,
  z `bestMatch` tylko przy pewności.
- **Jedno wejście tekstowe.** Frontend nie wie z góry, czy zdanie dotyczy książki, czy cyklu; rozstrzyga to jedno
  wywołanie Claude (`intent`). Odpowiedź `FIND_BOOK` jest zgodna wstecz (dodano tylko pole `intent`).
- **Deterministyczny kod zamiast agenta z narzędziami.** Intencji jest kilka i są stałe, więc router + zwykłe
  funkcje są tańsze, szybsze i testowalne. Model nie ma żadnych narzędzi.
- **Cykl wyłącznie z biblioteki użytkownika.** Lubimyczytac nie ma wyszukiwarki cykli (`/szukaj/cykle` nie zwraca
  linków), więc cykl „z zewnątrz" nie działa; wymagałoby to szukania książki z cyklu i czytania linku z karty.
- **Statusy zamiast wyjątków** dla pytań o cykle: frontend zawsze dostaje `200` z czytelnym powodem.

## Zmienione/dodane pliki (backend)

Wszystko w `library/` (ścieżki względem `src/main/java/net/focik/homeoffice/`).

| Warstwa | Pliki |
|---|---|
| API | `api/BookController` (`GET /search`, `POST /search/text`), `api/dto/BookSearchResultDto`, `BookTextSearchRequestDto`, `BookTextSearchResponseDto`, `api/mapper/ApiBookSearchResultMapper` |
| Porty | `port/primary/SearchBookUseCase`, `SearchSeriesUseCase`, `FindUserBookUseCase.findUserBooksInSeries`; `port/secondary/BookSearchPort`, `BookQueryExtractorPort`, `SeriesCatalogPort` |
| Domena | `BookSearchService`, `SeriesQueryService`, `SearchText`; model: `BookQuery`, `BookQueryIntent`, `BookSearchResult`, `BookTextSearchResult`, `SeriesAnswer`, `SeriesAnswerStatus`, `SeriesVolume`, `SeriesVolumeStatus`; wyjątek `BookQueryExtractionException` |
| Infrastruktura | `claude/ClaudeBookQueryExtractorAdapter`; `lubimyczytac/LubimyczytacBookSearchAdapter`, `LubimyczytacSeriesCatalogAdapter`; `bookbeat/BookbeatBookSearchAdapter` |
| Inne | `LibraryFacade`, `UserBookService` (książki użytkownika w cyklu), `utils/exceptions/ExceptionHandling` (503) |

Testy: `BookSearchServiceTest`, `SeriesQueryServiceTest`, `ClaudeBookQueryExtractorAdapterTest`,
`LubimyczytacBookSearchAdapterTest`, `LubimyczytacSeriesCatalogAdapterTest`, `BookbeatBookSearchAdapterTest`.
Testy sieciowe (na żywych serwisach) są pominięte; adaptery testowane na fragmentach prawdziwego HTML/JSON.

## Frontend

Szczegółowy prompt: [`BOOK_SEARCH_FRONTEND_PROMPT.md`](BOOK_SEARCH_FRONTEND_PROMPT.md). W skrócie: pole tekstowe z
mikrofonem (Web Speech API, `pl-PL`) → `POST /search/text` → zależnie od `intent` lista kandydatów albo widok cyklu.
Uwaga prywatnościowa: rozpoznawanie mowy w Chrome wysyła audio do serwerów Google.

## Znane ograniczenia / co dalej

- **Sortowanie numerów tomów.** `BookController` sortuje `bookInSeriesNo` jako tekst w `/series/{id}` i `/author/{id}`
  („10" przed „2"). Nowa logika cykli liczy numerycznie (`SeriesQueryService.parseNumber`).
- **Upolujebooka** nie jest źródłem (Cloudflare). **Google Books** (wymaga klucza API) mógłby być trzecim źródłem.
- **Cykle spoza biblioteki** i **cykle tylko z Upolujebooka/Legimi** (8 z 79) nie są obsługiwane.
- **Brak timeoutu dla zawieszonego źródła** w `BookSearchService` (patrz „Limity, blokady, wydajność"); naturalna
  poprawka to uruchamianie źródeł równolegle z `completeOnTimeout`, jak w `AudiobookAvailabilityService`.
- **`GET /search`** nie zwraca `bestMatch`.
- **Zdjęcie okładki** (Claude vision → ten sam potok) jest zaplanowane jako osobny etap.
- Poprawność promptu na prawdziwych frazach mówionych warto sprawdzić ręcznie; testy jednostkowe pokrywają parser
  odpowiedzi modelu, nie jego decyzje.
