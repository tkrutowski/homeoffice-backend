package net.focik.homeoffice.library.domain.model;

/**
 * Jeden tom cyklu.
 *
 * @param number numer tomu tak, jak podaje źródło (bywa ułamkowy, np. "15.5", albo {@code null}, gdy cykl nie jest numerowany)
 * @param url    adres strony książki w źródle; {@code null} dla książki, którą użytkownik ma w bibliotece, a której źródło nie zwróciło
 * @param bookId id książki w bibliotece użytkownika, jeśli ją ma; w przeciwnym razie {@code null}
 */
public record SeriesVolume(
        String number,
        String title,
        String authors,
        String cover,
        String url,
        SeriesVolumeStatus status,
        Integer bookId) {
}
