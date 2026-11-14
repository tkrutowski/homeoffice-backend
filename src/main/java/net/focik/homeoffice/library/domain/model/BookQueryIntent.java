package net.focik.homeoffice.library.domain.model;

/** Czego użytkownik chce w wolnym tekście. Domyślnie szuka pojedynczej książki. */
public enum BookQueryIntent {
    /** "znajdź mi książkę X autora Y" */
    FIND_BOOK,
    /** "znajdź wszystkie książki z cyklu X" */
    SERIES_ALL,
    /** "znajdź wszystkie książki z cyklu X, których nie przeczytałem" */
    SERIES_UNREAD,
    /** "znajdź następną książkę z cyklu X" (po ostatniej przeczytanej) */
    SERIES_NEXT;

    public boolean isSeries() {
        return this != FIND_BOOK;
    }
}
