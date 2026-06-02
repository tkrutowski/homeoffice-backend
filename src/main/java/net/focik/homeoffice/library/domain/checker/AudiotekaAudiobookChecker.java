package net.focik.homeoffice.library.domain.checker;

import lombok.extern.log4j.Log4j2;
import net.focik.homeoffice.library.domain.model.AudiobookPlatformResult;
import net.focik.homeoffice.library.infrastructure.dto.BookstoreDbDto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Log4j2
@Component
public class AudiotekaAudiobookChecker implements AudiobookChecker {

    @Override
    public boolean supports(String bookstoreUrl) {
        return bookstoreUrl != null && bookstoreUrl.contains("audioteka.com");
    }

    @Override
    public AudiobookPlatformResult check(String title, String author, BookstoreDbDto bookstore) {
        try {
            String searchQuery = encodeQuery(title, author);
            String searchUrl = "https://audioteka.com/pl/szukaj/?phrase=" + searchQuery;

            Document doc = Jsoup.connect(searchUrl)
                    .timeout(10000)
                    .userAgent("Mozilla/5.0")
                    .get();

            Elements results = doc.select("li.adtk-item");

            for (Element result : results) {
                String bookTitle = extractTitle(result);
                if (titleMatches(bookTitle, title)) {
                    String audioUrl = extractUrl(result);
                    if (audioUrl != null) {
                        return AudiobookPlatformResult.available(bookstore.getId(), "Audioteka", audioUrl);
                    }
                }
            }

            return AudiobookPlatformResult.unavailable(bookstore.getId(), "Audioteka");
        } catch (Exception e) {
            log.warn("Audioteka audiobook check failed: {}", e.getMessage());
            return AudiobookPlatformResult.error(bookstore.getId(), "Audioteka", e.getMessage());
        }
    }

    private String encodeQuery(String title, String author) {
        try {
            return URLEncoder.encode(title + " " + author, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return URLEncoder.encode(title, StandardCharsets.UTF_8);
        }
    }

    private String extractTitle(Element item) {
        Element titleEl = item.selectFirst("p.teaser_title__hDeCG");
        return titleEl != null ? titleEl.text() : "";
    }

    private String extractUrl(Element item) {
        Element linkEl = item.selectFirst("a.teaser_link__fxVFQ");
        if (linkEl != null) {
            String href = linkEl.attr("href");
            if (href.contains("/pl/audiobook/")) {
                return href.startsWith("http") ? href : "https://audioteka.com" + href;
            }
        }
        return null;
    }

    private boolean titleMatches(String found, String expected) {
        return found.toLowerCase().contains(expected.toLowerCase());
    }
}
