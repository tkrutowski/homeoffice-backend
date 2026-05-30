package net.focik.homeoffice.library.infrastructure.claude;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.library.domain.port.secondary.AiScraperPort;
import net.focik.homeoffice.library.domain.scraper.BookScraperDto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClaudeBookScraperAdapter implements AiScraperPort {

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            Jesteś ekspertem od analizowania stron internetowych z informacjami o książkach.
            Zwróć TYLKO obiekt JSON, bez żadnego dodatkowego tekstu ani formatowania markdown.
            Jeśli pole nie jest dostępne na stronie, ustaw je na null.
            Numer w serii: liczba lub ułamek dziesiętny (np. "1", "2.5"), bez tekstu.
            Autorzy: lista oddzielona przecinkami (np. "Jan Kowalski, Anna Nowak").
            Kategorie: lista oddzielona przecinkami (np. "Fantasy, Przygodowa").
            """;

    private static final String USER_PROMPT = """
            Przeanalizuj treść tej strony internetowej i zwróć dane JSON o książce:
            {
              "title": "tytuł książki",
              "authors": "autor1, autor2",
              "series": "nazwa serii lub null",
              "seriesURL": "bezpośredni URL strony serii lub null",
              "bookInSeriesNo": "numer książki w serii lub null",
              "categories": "kategoria1, kategoria2 lub null",
              "description": "opis książki lub null",
              "cover": "bezpośredni URL obrazka okładki lub null"
            }

            Treść strony:
            """;

    @Override
    public BookScraperDto findBookByUrl(String url) {
        log.debug("Extracting book data from URL via Claude: {}", url);
        try {
            String pageContent = extractRelevantContent(url);
            log.debug("Extracted {} characters of relevant content from URL: {}", pageContent.length(), url);

            String json = callClaude(pageContent);
            BookScraperDto result = parseResponse(json);
            log.debug("Successfully extracted book: {}", result.getTitle());
            return result;
        } catch (Exception e) {
            log.error("Failed to extract book data from URL: {}", url, e);
            return BookScraperDto.createEmptyDto();
        }
    }

    private String extractRelevantContent(String url) throws Exception {
        Document document = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(10_000)
                .get();

        Element body = document.body();
        for (String tag : List.of("nav", "script", "footer", "header", "style", "iframe", "noscript", "aside")) {
            body.select(tag).remove();
        }
        return body.text();
    }

    private String callClaude(String pageContent) {
        SystemMessage systemMessage = new SystemMessage(SYSTEM_PROMPT);
        UserMessage userMessage = new UserMessage(USER_PROMPT + pageContent);
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        String response = chatModel.call(prompt).getResult().getOutput().getContent();
        log.debug("Claude response length: {} characters", response.length());
        return response;
    }

    private BookScraperDto parseResponse(String jsonResponse) {
        try {
            String cleaned = cleanJsonResponse(jsonResponse);
            return objectMapper.readValue(cleaned, BookScraperDto.class);
        } catch (Exception e) {
            log.error("Failed to parse Claude response as BookScraperDto", e);
            throw new RuntimeException("Failed to parse book JSON from Claude: " + e.getMessage(), e);
        }
    }

    private String cleanJsonResponse(String response) {
        if (response == null) {
            throw new IllegalArgumentException("Claude response is null");
        }
        response = response.trim();
        if (response.startsWith("```json")) {
            response = response.substring(7);
        }
        if (response.startsWith("```")) {
            response = response.substring(3);
        }
        if (response.endsWith("```")) {
            response = response.substring(0, response.length() - 3);
        }
        return response.trim();
    }
}
