package net.focik.homeoffice.library.domain.checker;

import net.focik.homeoffice.library.domain.model.AudiobookPlatformResult;
import net.focik.homeoffice.library.infrastructure.dto.BookstoreDbDto;

public interface AudiobookChecker {
    boolean supports(String bookstoreUrl);

    AudiobookPlatformResult check(String title, String author, BookstoreDbDto bookstore);
}
