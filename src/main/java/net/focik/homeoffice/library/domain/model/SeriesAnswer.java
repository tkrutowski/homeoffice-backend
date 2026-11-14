package net.focik.homeoffice.library.domain.model;

import java.util.List;

/**
 * Odpowiedź na pytanie o cykl.
 *
 * @param seriesTitle tytuł cyklu z biblioteki użytkownika (null, gdy cyklu nie rozpoznano)
 * @param options     tytuły pasujących cykli przy {@link SeriesAnswerStatus#AMBIGUOUS_SERIES}
 * @param volumes     tomy według numerów: wszystkie ({@code SERIES_ALL}) albo tylko nieprzeczytane ({@code SERIES_UNREAD});
 *                    puste przy {@code SERIES_NEXT}
 * @param lastRead    najwyższy przeczytany tom, przy {@code SERIES_NEXT}
 * @param next        tom do przeczytania jako następny, przy {@code SERIES_NEXT}
 */
public record SeriesAnswer(
        SeriesAnswerStatus status,
        String seriesTitle,
        String seriesUrl,
        List<String> options,
        List<SeriesVolume> volumes,
        SeriesVolume lastRead,
        SeriesVolume next) {

    public static SeriesAnswer of(SeriesAnswerStatus status) {
        return new SeriesAnswer(status, null, null, List.of(), List.of(), null, null);
    }

    public static SeriesAnswer of(SeriesAnswerStatus status, String seriesTitle) {
        return new SeriesAnswer(status, seriesTitle, null, List.of(), List.of(), null, null);
    }

    public static SeriesAnswer ambiguous(List<String> options) {
        return new SeriesAnswer(SeriesAnswerStatus.AMBIGUOUS_SERIES, null, null, options, List.of(), null, null);
    }
}
