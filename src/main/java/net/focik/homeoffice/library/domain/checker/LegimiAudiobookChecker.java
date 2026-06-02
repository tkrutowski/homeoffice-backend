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
public class LegimiAudiobookChecker implements AudiobookChecker {

    @Override
    public boolean supports(String bookstoreUrl) {
        return bookstoreUrl != null && bookstoreUrl.contains("legimi.pl");
    }

    @Override
    public AudiobookPlatformResult check(String title, String author, BookstoreDbDto bookstore) {
        try {
            String searchQuery = encodeQuery(title, author);
            String searchUrl = "https://www.legimi.pl/katalog/?searchphrase=" + searchQuery + "&sort=score";

            Document doc = Jsoup.connect(searchUrl)
                    .timeout(10000)
                    .userAgent("Mozilla/5.0")
                    .get();

            Elements results = doc.select("section.book-box");

            for (Element result : results) {
                if (!isAvailable(result)) {
                    continue;
                }
                if (!hasAudiobook(result)) {
                    continue;
                }
                String bookTitle = extractTitle(result);
                if (titleMatches(bookTitle, title)) {
                    String audioUrl = extractUrl(result);
                    if (audioUrl != null) {
                        return AudiobookPlatformResult.available(bookstore.getId(), "Legimi", audioUrl);
                    }
                }
            }

            return AudiobookPlatformResult.unavailable(bookstore.getId(), "Legimi");
        } catch (Exception e) {
            log.warn("Legimi audiobook check failed: {}", e.getMessage());
            return AudiobookPlatformResult.error(bookstore.getId(), "Legimi", e.getMessage());
        }
    }

    private boolean isAvailable(Element bookBox) {
        Element imgWrap = bookBox.selectFirst(".book-img-wrap");
        return imgWrap != null && !imgWrap.hasClass("not-available");
    }

    private boolean hasAudiobook(Element bookBox) {
        return bookBox.selectFirst("i.icon-book-format-audiobook") != null;
    }

    private String encodeQuery(String title, String author) {
        try {
            return URLEncoder.encode(title + " " + author, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return URLEncoder.encode(title, StandardCharsets.UTF_8);
        }
    }

    private String extractTitle(Element bookBox) {
        Element titleEl = bookBox.selectFirst("a.book-title.clampBookTitle");
        return titleEl != null ? titleEl.text() : "";
    }

    private String extractUrl(Element bookBox) {
        Element linkEl = bookBox.selectFirst(".book-img-wrap a");
        if (linkEl != null) {
            String href = linkEl.attr("href");
            return href.startsWith("http") ? href : "https://www.legimi.pl" + href;
        }
        return null;
    }

    private boolean titleMatches(String found, String expected) {
        return found.toLowerCase().contains(expected.toLowerCase());
    }
}
