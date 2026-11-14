package net.focik.homeoffice.library.infrastructure.claude;

import net.focik.homeoffice.library.domain.exception.BookQueryExtractionException;
import net.focik.homeoffice.library.domain.model.BookQuery;
import net.focik.homeoffice.library.domain.model.BookQueryIntent;
import net.focik.homeoffice.library.domain.model.EditionType;
import net.focik.homeoffice.library.domain.model.OwnershipStatus;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClaudeBookQueryExtractorAdapterTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private static BookQuery parse(String response) {
        return ClaudeBookQueryExtractorAdapter.parse(response, MAPPER);
    }

    @Test
    void parsesAllFields() {
        BookQuery q = parse("""
                {"title": "Rodzinny interes", "author": "Chmielarz", "editionType": "AUDIOBOOK", "ownershipStatus": "WANT"}""");

        assertEquals("Rodzinny interes", q.title());
        assertEquals("Chmielarz", q.author());
        assertEquals(EditionType.AUDIOBOOK, q.editionType());
        assertEquals(OwnershipStatus.WANT, q.ownershipStatus());
    }

    @Test
    void toleratesMarkdownFenceAndSurroundingProse() {
        BookQuery q = parse("Oto wynik:\n```json\n{\"title\": \"Mordownia\", \"author\": null}\n```\nMiłego dnia!");

        assertEquals("Mordownia", q.title());
        assertNull(q.author());
    }

    @Test
    void nullLikeValuesAndUnknownEnumsBecomeNull() {
        BookQuery q = parse("""
                {"title": "Tytuł", "author": "null", "editionType": "PAPEROWA", "ownershipStatus": "", "extra": 1}""");

        assertEquals("Tytuł", q.title());
        assertNull(q.author());
        assertNull(q.editionType());
        assertNull(q.ownershipStatus());
    }

    @Test
    void enumsAreCaseInsensitiveAndNonTextValuesIgnored() {
        BookQuery q = parse("{\"title\": 123, \"author\": \"  Nowak \", \"editionType\": \"ebook\", \"ownershipStatus\": \"have\"}");

        assertNull(q.title());
        assertEquals("Nowak", q.author());
        assertEquals(EditionType.EBOOK, q.editionType());
        assertEquals(OwnershipStatus.HAVE, q.ownershipStatus());
    }

    @Test
    void overlongFieldsAreTruncated() {
        BookQuery q = parse("{\"title\": \"" + "a".repeat(500) + "\"}");

        assertEquals(200, q.title().length());
    }

    @Test
    void responseWithoutJsonObjectIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> parse("Nie rozumiem polecenia."));
    }

    @Test
    void extractReturnsParsedQueryFromModelResponse() {
        ChatModel chatModel = mock(ChatModel.class, RETURNS_DEEP_STUBS);
        when(chatModel.call(any(Prompt.class)).getResult().getOutput().getText())
                .thenReturn("{\"title\": \"Rodzinny interes\", \"author\": \"Chmielarz\"}");

        BookQuery q = new ClaudeBookQueryExtractorAdapter(chatModel, MAPPER).extract("rodzinny interes chmielarz");

        assertEquals("Rodzinny interes", q.title());
        assertEquals("Chmielarz", q.author());
    }

    @Test
    void extractWrapsModelFailureInDomainException() {
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("529 overloaded"));

        assertThrows(BookQueryExtractionException.class,
                () -> new ClaudeBookQueryExtractorAdapter(chatModel, MAPPER).extract("cokolwiek"));
    }

    // --- intencje dotyczące cyklu ---

    @Test
    void parsesSeriesIntentWithSeriesName() {
        BookQuery q = parse("{\"intent\": \"SERIES_NEXT\", \"series\": \"Bezimienny\", \"title\": null}");

        assertEquals(BookQueryIntent.SERIES_NEXT, q.intent());
        assertEquals("Bezimienny", q.series());
        assertTrue(q.intent().isSeries());
    }

    @Test
    void missingOrUnknownIntentDefaultsToFindBook() {
        assertEquals(BookQueryIntent.FIND_BOOK, parse("{\"title\": \"Mordownia\"}").intent());
        assertEquals(BookQueryIntent.FIND_BOOK, parse("{\"intent\": \"COS_INNEGO\", \"title\": \"Mordownia\"}").intent());
        assertEquals(BookQueryIntent.FIND_BOOK, parse("{\"intent\": null, \"title\": \"Mordownia\"}").intent());
    }

    @Test
    void seriesNameIsIgnoredForFindBookIntent() {
        BookQuery q = parse("{\"intent\": \"FIND_BOOK\", \"title\": \"Mordownia\", \"series\": \"Coś\"}");

        assertNull(q.series());
        assertEquals("Mordownia", q.title());
    }

    @Test
    void seriesIntentWithoutSeriesNameKeepsIntentAndNullSeries() {
        BookQuery q = parse("{\"intent\": \"SERIES_ALL\", \"series\": \"null\"}");

        assertEquals(BookQueryIntent.SERIES_ALL, q.intent());
        assertNull(q.series());
        assertFalse(q.hasSeries());
    }

    @Test
    void intentIsCaseInsensitive() {
        assertEquals(BookQueryIntent.SERIES_UNREAD, parse("{\"intent\": \"series_unread\", \"series\": \"X\"}").intent());
    }
}
