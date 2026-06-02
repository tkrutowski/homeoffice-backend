package net.focik.homeoffice.library.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.focik.homeoffice.library.domain.checker.AudiobookChecker;
import net.focik.homeoffice.library.domain.model.AudiobookAvailability;
import net.focik.homeoffice.library.domain.model.AudiobookPlatformResult;
import net.focik.homeoffice.library.domain.model.Author;
import net.focik.homeoffice.library.domain.model.Book;
import net.focik.homeoffice.library.domain.model.Bookstore;
import net.focik.homeoffice.library.infrastructure.dto.BookstoreDbDto;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class AudiobookAvailabilityService  {

    private final List<AudiobookChecker> audiobookCheckers;

    public AudiobookAvailability checkAvailability(Book book, List<Bookstore> allBookstores) {
        log.info("Found {} bookstores", allBookstores.size());
        allBookstores.forEach(bs -> log.info("Bookstore: {} ({})", bs.getName(), bs.getUrl()));

        String title = book.getTitle();
        String author = extractFirstAuthor(book.getAuthors());

        List<AudiobookPlatformResult> results = allBookstores.stream()
                .flatMap(bookstore -> audiobookCheckers.stream()
                        .filter(checker -> checker.supports(bookstore.getUrl()))
                        .findFirst()
                        .map(checker -> runCheckerWithTimeout(checker, title, author, bookstore))
                        .stream())
                .collect(Collectors.toList());

        return AudiobookAvailability.builder()
                .bookId(book.getId())
                .title(title)
                .author(author)
                .results(results)
                .checkedAt(LocalDateTime.now())
                .build();
    }

    private AudiobookPlatformResult runCheckerWithTimeout(AudiobookChecker checker, String title, String author, Bookstore bookstore) {
        try {
            return CompletableFuture.supplyAsync(() -> {
                        BookstoreDbDto dbDto = new BookstoreDbDto();
                        dbDto.setId(bookstore.getId());
                        dbDto.setName(bookstore.getName());
                        dbDto.setUrl(bookstore.getUrl());
                        return checker.check(title, author, dbDto);
                    })
                    .completeOnTimeout(AudiobookPlatformResult.timeout(bookstore.getId(), getPlatformName(checker)), 13, TimeUnit.SECONDS)
                    .get(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Error checking audiobook availability for {}: {}", bookstore.getName(), e.getMessage());
            return AudiobookPlatformResult.error(bookstore.getId(), getPlatformName(checker), e.getMessage());
        }
    }

    private String extractFirstAuthor(java.util.Set<Author> authors) {
        if (authors == null || authors.isEmpty()) {
            return "";
        }
        Author author = authors.iterator().next();
        return author.getFirstName() + " " + author.getLastName();
    }

    private String getPlatformName(AudiobookChecker checker) {
        String className = checker.getClass().getSimpleName();
        return className.replace("AudiobookChecker", "");
    }
}
