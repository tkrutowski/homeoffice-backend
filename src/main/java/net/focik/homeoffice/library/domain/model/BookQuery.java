package net.focik.homeoffice.library.domain.model;

/**
 * Zapytanie o książkę wyciągnięte z wolnego tekstu (np. podyktowanego). Wszystkie pola mogą być
 * {@code null}, gdy w tekście ich nie było - nigdy nie zgadujemy autora ani wydania.
 *
 * @param editionType     podpowiedź rodzaju wydania ("audiobook", "ebook"...), do wstępnego wypełnienia formularza
 * @param ownershipStatus podpowiedź statusu ("mam", "chcę kupić", "tylko czytam"...)
 */
public record BookQuery(
        String title,
        String author,
        EditionType editionType,
        OwnershipStatus ownershipStatus) {

    public static BookQuery empty() {
        return new BookQuery(null, null, null, null);
    }

    public boolean hasTitle() {
        return title != null && !title.isBlank();
    }
}
