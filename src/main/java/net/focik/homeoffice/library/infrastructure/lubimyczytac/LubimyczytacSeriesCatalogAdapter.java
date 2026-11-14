package net.focik.homeoffice.library.infrastructure.lubimyczytac;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.library.domain.model.BookSearchResult;
import net.focik.homeoffice.library.domain.port.secondary.SeriesCatalogPort;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lista tomów cyklu z lubimyczytac.pl ({@code /cykl/ID/...} i {@code /seria/ID/...}).
 * <p>
 * Strona cyklu pokazuje tylko pierwsze 20 pozycji, resztę dociąga skrypt przez
 * {@code POST /series/getMoreBooks} (strony po 20, w odpowiedzi {@code data.left} = ile jeszcze zostało,
 * {@code data.content} = HTML tych samych kart co w wyszukiwarce). Używamy tego API od pierwszej strony,
 * żeby mieć jedną ścieżkę. Wynik jest trzymany w pamięci {@link #CACHE_TTL}, bo lista cyklu rzadko się
 * zmienia, a każde pobranie to kilka żądań do serwisu, który blokuje zbyt szybkich klientów.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LubimyczytacSeriesCatalogAdapter implements SeriesCatalogPort {

    private static final String API_URL = "https://lubimyczytac.pl/series/getMoreBooks";
    private static final Pattern SERIES_URL = Pattern.compile("^https?://(?:www\\.)?lubimyczytac\\.pl/(cykl|seria)/(\\d+)(?:[/?#].*)?$");
    private static final int MAX_PAGES = 10;
    private static final Duration CACHE_TTL = Duration.ofHours(6);
    private static final int MIN_PAUSE_MS = 500;
    private static final int PAUSE_JITTER_MS = 500;

    private final ObjectMapper objectMapper;
    private final Map<String, CachedVolumes> cache = new ConcurrentHashMap<>();

    private record CachedVolumes(Instant fetchedAt, List<BookSearchResult> volumes) {
    }

    /** Jedna strona odpowiedzi API: karty oraz ile pozycji zostało do pobrania. */
    record Page(List<BookSearchResult> volumes, int left) {
    }

    @Override
    public boolean supports(String seriesUrl) {
        return seriesUrl != null && SERIES_URL.matcher(seriesUrl.trim()).matches();
    }

    @Override
    public List<BookSearchResult> findVolumes(String seriesUrl) {
        Matcher m = SERIES_URL.matcher(seriesUrl == null ? "" : seriesUrl.trim());
        if (!m.matches()) {
            throw new IllegalArgumentException("Not a Lubimyczytac series url: " + seriesUrl);
        }
        // /cykl/ -> viewType=cykle, /seria/ (seria wydawnicza) -> viewType=serie
        String viewType = m.group(1).equals("cykl") ? "cykle" : "serie";
        String seriesId = m.group(2);
        String cacheKey = viewType + ":" + seriesId;

        CachedVolumes cached = cache.get(cacheKey);
        if (cached != null && cached.fetchedAt().plus(CACHE_TTL).isAfter(Instant.now())) {
            log.debug("Lubimyczytac series {} served from cache ({} volumes)", cacheKey, cached.volumes().size());
            return cached.volumes();
        }

        List<BookSearchResult> volumes = fetchAllPages(seriesId, viewType);
        if (!volumes.isEmpty()) {
            cache.put(cacheKey, new CachedVolumes(Instant.now(), volumes));
        }
        return volumes;
    }

    private List<BookSearchResult> fetchAllPages(String seriesId, String viewType) {
        Set<String> seenUrls = new LinkedHashSet<>();
        List<BookSearchResult> all = new ArrayList<>();
        for (int page = 1; page <= MAX_PAGES; page++) {
            if (page > 1) {
                pause();
            }
            Page result = fetchPage(seriesId, viewType, page);
            for (BookSearchResult volume : result.volumes()) {
                if (seenUrls.add(volume.url())) {
                    all.add(volume);
                }
            }
            if (result.left() <= 0 || result.volumes().isEmpty()) {
                break;
            }
        }
        log.debug("Lubimyczytac series {}:{} fetched, {} volumes", viewType, seriesId, all.size());
        return all;
    }

    private Page fetchPage(String seriesId, String viewType, int page) {
        try {
            String body = Jsoup.connect(API_URL)
                    .method(Connection.Method.POST)
                    .data("serieId", seriesId)
                    .data("viewType", viewType)
                    .data("phrase", "")
                    .data("sortBy", "default")
                    .data("page", String.valueOf(page))
                    .header("X-Requested-With", "XMLHttpRequest")
                    .userAgent("Mozilla/5.0")
                    .ignoreContentType(true)
                    .timeout(10_000)
                    .execute()
                    .body();
            return parsePage(body, objectMapper);
        } catch (IOException e) {
            throw new IllegalStateException("Lubimyczytac series page " + page + " failed: " + e.getMessage(), e);
        }
    }

    static Page parsePage(String json, ObjectMapper objectMapper) {
        JsonNode data = objectMapper.readTree(json).path("data");
        // przy błędzie serwis zwraca "data": [] zamiast obiektu
        String content = data.path("content").asString("");
        if (content.isBlank()) {
            return new Page(List.of(), 0);
        }
        Document fragment = Jsoup.parseBodyFragment(content, "https://lubimyczytac.pl");
        return new Page(LubimyczytacBookSearchAdapter.parseCards(fragment.select("div.book-card")),
                data.path("left").asInt(0));
    }

    private void pause() {
        try {
            Thread.sleep(MIN_PAUSE_MS + ThreadLocalRandom.current().nextInt(PAUSE_JITTER_MS + 1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while fetching series pages", e);
        }
    }
}
