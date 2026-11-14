package net.focik.homeoffice.library.domain.model;

/**
 * Zapytanie o książkę wyciągnięte z wolnego tekstu (np. podyktowanego). Wszystkie pola poza {@code intent}
 * mogą być {@code null}, gdy w tekście ich nie było - nigdy nie zgadujemy autora ani wydania.
 *
 * @param editionType     podpowiedź rodzaju wydania ("audiobook", "ebook"...), do wstępnego wypełnienia formularza
 * @param ownershipStatus podpowiedź statusu ("mam", "chcę kupić", "tylko czytam"...)
 * @param intent          czego użytkownik chce; domyślnie {@link BookQueryIntent#FIND_BOOK}
 * @param series          nazwa cyklu w mianowniku, dla intencji dotyczących cyklu
 */
public record BookQuery(
        String title,
        String author,
        EditionType editionType,
        OwnershipStatus ownershipStatus,
        BookQueryIntent intent,
        String series) {

    /** Zapytanie o pojedynczą książkę (bez cyklu). */
    public BookQuery(String title, String author, EditionType editionType, OwnershipStatus ownershipStatus) {
        this(title, author, editionType, ownershipStatus, BookQueryIntent.FIND_BOOK, null);
    }

    public BookQuery {
        if (intent == null) {
            intent = BookQueryIntent.FIND_BOOK;
        }
    }

    public static BookQuery empty() {
        return new BookQuery(null, null, null, null);
    }

    public boolean hasTitle() {
        return title != null && !title.isBlank();
    }

    public boolean hasSeries() {
        return series != null && !series.isBlank();
    }
}
