package net.focik.homeoffice.library.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.library.domain.model.Book;
import net.focik.homeoffice.library.domain.model.BookQueryIntent;
import net.focik.homeoffice.library.domain.model.BookSearchResult;
import net.focik.homeoffice.library.domain.model.ReadingStatus;
import net.focik.homeoffice.library.domain.model.Series;
import net.focik.homeoffice.library.domain.model.SeriesAnswer;
import net.focik.homeoffice.library.domain.model.SeriesAnswerStatus;
import net.focik.homeoffice.library.domain.model.SeriesVolume;
import net.focik.homeoffice.library.domain.model.SeriesVolumeStatus;
import net.focik.homeoffice.library.domain.model.UserBook;
import net.focik.homeoffice.library.domain.port.primary.FindUserBookUseCase;
import net.focik.homeoffice.library.domain.port.primary.SearchSeriesUseCase;
import net.focik.homeoffice.library.domain.port.secondary.SeriesCatalogPort;
import net.focik.homeoffice.library.domain.port.secondary.SeriesRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Odpowiada na pytania o cykl: pełna lista tomów ze statusem przeczytania, tylko nieprzeczytane, następny tom.
 * Cykl jest szukany w bibliotece użytkownika (tam jest adres strony cyklu), a lista tomów pochodzi ze źródła
 * ({@link SeriesCatalogPort}) i jest nakładana na książki użytkownika.
 */
@Slf4j
@Service
@RequiredArgsConstructor
class SeriesQueryService implements SearchSeriesUseCase {

    private static final int MAX_OPTIONS = 5;
    private static final String URL_SEPARATOR = ";;";

    // punktacja dopasowania nazwy cyklu
    private static final int SCORE_EXACT = 100;
    private static final int SCORE_ALL_TOKENS = 50;
    private static final int SCORE_SAME_TOKEN_COUNT = 10;

    private final SeriesRepository seriesRepository;
    private final FindUserBookUseCase findUserBookUseCase;
    private final List<SeriesCatalogPort> catalogs;

    @Override
    public SeriesAnswer answer(BookQueryIntent intent, String seriesName, String userName) {
        if (intent == null || !intent.isSeries()) {
            throw new IllegalArgumentException("Intent is not a series intent: " + intent);
        }
        if (seriesName == null || seriesName.isBlank()) {
            return SeriesAnswer.of(SeriesAnswerStatus.SERIES_NAME_MISSING);
        }

        List<Series> matches = matchSeries(seriesRepository.findAll(), seriesName);
        if (matches.isEmpty()) {
            log.debug("No series in library matches '{}'", seriesName);
            return SeriesAnswer.of(SeriesAnswerStatus.SERIES_NOT_FOUND);
        }
        if (matches.size() > 1) {
            return SeriesAnswer.ambiguous(matches.stream().map(Series::getTitle).limit(MAX_OPTIONS).toList());
        }
        Series series = matches.get(0);

        Source source = pickSource(series.getUrl());
        if (source == null) {
            log.debug("Series '{}' has no supported source among urls: {}", series.getTitle(), series.getUrl());
            return SeriesAnswer.of(SeriesAnswerStatus.NO_SUPPORTED_SOURCE, series.getTitle());
        }

        List<BookSearchResult> scraped;
        try {
            scraped = source.port().findVolumes(source.url());
        } catch (Exception e) {
            log.warn("Series volumes source failed for '{}' ({}): {}", series.getTitle(), source.url(), e.getMessage());
            return SeriesAnswer.of(SeriesAnswerStatus.SOURCE_UNAVAILABLE, series.getTitle());
        }
        if (scraped.isEmpty()) {
            log.warn("Series volumes source returned no volumes for '{}' ({})", series.getTitle(), source.url());
            return SeriesAnswer.of(SeriesAnswerStatus.SOURCE_UNAVAILABLE, series.getTitle());
        }

        List<UserBook> userBooks = findUserBookUseCase.findUserBooksInSeries(userName, series.getId());
        List<SeriesVolume> volumes = mergeWithLibrary(scraped, userBooks);

        return switch (intent) {
            case SERIES_ALL -> volumesAnswer(series, source, volumes);
            case SERIES_UNREAD -> volumesAnswer(series, source,
                    volumes.stream().filter(v -> v.status() != SeriesVolumeStatus.READ).toList());
            case SERIES_NEXT -> nextAnswer(series, source, volumes);
            case FIND_BOOK -> throw new IllegalStateException("unreachable");
        };
    }

    // --- odpowiedzi ---

    private SeriesAnswer volumesAnswer(Series series, Source source, List<SeriesVolume> volumes) {
        return new SeriesAnswer(SeriesAnswerStatus.OK, series.getTitle(), source.url(), List.of(), volumes, null, null);
    }

    /**
     * Następny = pierwszy tom o numerze wyższym niż najwyższy przeczytany. Tom "czytam" ({@code READING}) liczy się
     * jak przeczytany - pytając o następny, użytkownik chce wiedzieć, co czytać PO bieżącym, więc wtedy to on jest
     * {@code lastRead}. Numery bywają ułamkowe (opowiadania, "15.5") i zdarzają się duplikaty - kolejność ustala
     * numer, przy remisie kolejność źródła. Gdy nic nie przeczytano, następny to pierwszy tom od numeru 1 (numer 0
     * to zwykle prequel/dodatek).
     */
    private SeriesAnswer nextAnswer(Series series, Source source, List<SeriesVolume> volumes) {
        List<SeriesVolume> numbered = volumes.stream().filter(v -> parseNumber(v.number()) != null).toList();
        if (numbered.isEmpty()) {
            return SeriesAnswer.of(SeriesAnswerStatus.NOT_ORDERED, series.getTitle());
        }

        SeriesVolume lastRead = null;
        for (SeriesVolume v : numbered) { // posortowane rosnąco, więc ostatni READ/READING to najwyższy numer
            if (v.status() == SeriesVolumeStatus.READ || v.status() == SeriesVolumeStatus.READING) {
                lastRead = v;
            }
        }

        SeriesVolume next;
        SeriesAnswerStatus status = SeriesAnswerStatus.OK;
        if (lastRead == null) {
            next = numbered.stream()
                    .filter(v -> parseNumber(v.number()).compareTo(BigDecimal.ONE) >= 0)
                    .findFirst()
                    .orElse(numbered.get(0));
        } else {
            BigDecimal lastNumber = parseNumber(lastRead.number());
            next = numbered.stream()
                    .filter(v -> parseNumber(v.number()).compareTo(lastNumber) > 0)
                    .findFirst()
                    .orElse(null);
            if (next == null) {
                status = SeriesAnswerStatus.NO_NEXT_VOLUME;
            }
        }
        return new SeriesAnswer(status, series.getTitle(), source.url(), List.of(), List.of(), lastRead, next);
    }

    // --- dopasowanie cyklu ---

    /** Najlepiej pasujące cykle: dokładny tytuł wygrywa ze wszystkim; tytuł zawierający wszystkie słowa to remis. */
    private List<Series> matchSeries(List<Series> all, String name) {
        List<String> queryTokens = SearchText.tokens(name);
        if (queryTokens.isEmpty()) {
            return List.of();
        }
        String queryNormalized = String.join(" ", queryTokens);

        int best = 0;
        List<Series> top = new ArrayList<>();
        for (Series s : all) {
            if (s.getTitle() == null) {
                continue;
            }
            int score = matchScore(queryTokens, queryNormalized, s.getTitle());
            if (score == 0 || score < best) {
                continue;
            }
            if (score > best) {
                best = score;
                top.clear();
            }
            top.add(s);
        }
        return top;
    }

    private int matchScore(List<String> queryTokens, String queryNormalized, String title) {
        List<String> titleTokens = SearchText.tokens(title);
        if (String.join(" ", titleTokens).equals(queryNormalized)) {
            return SCORE_EXACT;
        }
        boolean allMatch = queryTokens.stream().allMatch(q -> titleTokens.stream().anyMatch(t -> tokenMatches(q, t)));
        if (!allMatch) {
            return 0;
        }
        return SCORE_ALL_TOKENS + (titleTokens.size() == queryTokens.size() ? SCORE_SAME_TOKEN_COUNT : 0);
    }

    /**
     * Słowa pasują, gdy są równe albo różnią się tylko końcówką fleksyjną ("Bezimiennym" ~ "Bezimienny",
     * "wiedźminie" ~ "wiedźmin"). Krótkie słowa (do 4 liter) muszą być równe, żeby "star" nie łapało "starship".
     */
    private boolean tokenMatches(String a, String b) {
        if (a.equals(b)) {
            return true;
        }
        int shorter = Math.min(a.length(), b.length());
        if (shorter < 5 || Math.abs(a.length() - b.length()) > 3) {
            return false;
        }
        int common = 0;
        while (common < shorter && a.charAt(common) == b.charAt(common)) {
            common++;
        }
        return common >= shorter - 2;
    }

    // --- źródło tomów ---

    private record Source(String url, SeriesCatalogPort port) {
    }

    /** Adres zapisany w bibliotece to lista rozdzielona ";;" (różne serwisy, bywają duplikaty i pusty ogon). */
    private Source pickSource(String urlField) {
        if (urlField == null || urlField.isBlank()) {
            return null;
        }
        List<String> urls = Arrays.stream(urlField.split(URL_SEPARATOR))
                .map(String::trim)
                .filter(u -> !u.isEmpty())
                .distinct()
                .toList();
        for (String url : urls) {
            for (SeriesCatalogPort port : catalogs) {
                if (port.supports(url)) {
                    return new Source(url, port);
                }
            }
        }
        return null;
    }

    // --- nakładanie biblioteki użytkownika na listę tomów ---

    /**
     * Każdy tom ze źródła dostaje status z książek użytkownika (po tytule, a gdy tytuł się różni - po numerze,
     * ale tylko jeśli numer jest w cyklu unikalny, bo zdarzają się zdublowane numery). Książki użytkownika,
     * których źródło nie zwróciło, dopisujemy na koniec, żeby nie znikały z odpowiedzi ani z ustalania "ostatnio przeczytanego".
     */
    private List<SeriesVolume> mergeWithLibrary(List<BookSearchResult> scraped, List<UserBook> userBooks) {
        Map<Integer, List<UserBook>> byBook = new LinkedHashMap<>();
        for (UserBook ub : userBooks) {
            if (ub.getBook() != null && ub.getBook().getId() != null) {
                byBook.computeIfAbsent(ub.getBook().getId(), id -> new ArrayList<>()).add(ub);
            }
        }

        Map<BigDecimal, Long> numberCounts = scraped.stream()
                .map(v -> parseNumber(v.bookInSeriesNo()))
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(BigDecimal::stripTrailingZeros, Collectors.counting()));

        Set<Integer> usedBooks = new HashSet<>();
        List<SeriesVolume> result = new ArrayList<>();
        for (BookSearchResult volume : scraped) {
            Integer bookId = findMatchingBook(volume, byBook, usedBooks, numberCounts);
            SeriesVolumeStatus status = SeriesVolumeStatus.NOT_IN_LIBRARY;
            if (bookId != null) {
                usedBooks.add(bookId);
                status = statusOf(byBook.get(bookId));
            }
            result.add(new SeriesVolume(volume.bookInSeriesNo(), volume.title(), volume.authors(),
                    volume.cover(), volume.url(), status, bookId));
        }

        for (Map.Entry<Integer, List<UserBook>> entry : byBook.entrySet()) {
            if (usedBooks.contains(entry.getKey())) {
                continue;
            }
            Book book = entry.getValue().get(0).getBook();
            String number = book.getBookInSeriesNo() == null || book.getBookInSeriesNo().isBlank()
                    ? null : book.getBookInSeriesNo().trim();
            result.add(new SeriesVolume(number, book.getTitle(), authorsOf(book), book.getCover(), null,
                    statusOf(entry.getValue()), book.getId()));
        }

        // stabilny: bez numeru na koniec, przy remisie zostaje kolejność ze źródła
        result.sort(Comparator.comparing((SeriesVolume v) -> parseNumber(v.number()),
                Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    private Integer findMatchingBook(BookSearchResult volume, Map<Integer, List<UserBook>> byBook,
                                     Set<Integer> usedBooks, Map<BigDecimal, Long> numberCounts) {
        String volumeTitle = SearchText.loose(volume.title());
        for (Map.Entry<Integer, List<UserBook>> entry : byBook.entrySet()) {
            if (!usedBooks.contains(entry.getKey())
                    && SearchText.loose(entry.getValue().get(0).getBook().getTitle()).equals(volumeTitle)) {
                return entry.getKey();
            }
        }

        BigDecimal number = parseNumber(volume.bookInSeriesNo());
        if (number == null || numberCounts.getOrDefault(number.stripTrailingZeros(), 0L) != 1) {
            return null;
        }
        for (Map.Entry<Integer, List<UserBook>> entry : byBook.entrySet()) {
            BigDecimal bookNumber = parseNumber(entry.getValue().get(0).getBook().getBookInSeriesNo());
            if (!usedBooks.contains(entry.getKey()) && bookNumber != null && bookNumber.compareTo(number) == 0) {
                return entry.getKey();
            }
        }
        return null;
    }

    private SeriesVolumeStatus statusOf(List<UserBook> userBooks) {
        if (userBooks.stream().anyMatch(ub -> ub.getReadingStatus() == ReadingStatus.READ)) {
            return SeriesVolumeStatus.READ;
        }
        if (userBooks.stream().anyMatch(ub -> ub.getReadingStatus() == ReadingStatus.READ_NOW)) {
            return SeriesVolumeStatus.READING;
        }
        return SeriesVolumeStatus.ON_SHELF;
    }

    private String authorsOf(Book book) {
        if (book.getAuthors() == null) {
            return null;
        }
        String authors = book.getAuthors().stream()
                .map(a -> (nullToEmpty(a.getFirstName()) + " " + nullToEmpty(a.getLastName())).trim())
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(", "));
        return authors.isEmpty() ? null : authors;
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /** "2", "15.5", "2,5", "00" -> liczba; brak numeru lub tekst -> null. */
    static BigDecimal parseNumber(String number) {
        if (number == null || number.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(number.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
