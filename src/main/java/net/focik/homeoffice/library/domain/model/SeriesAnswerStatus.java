package net.focik.homeoffice.library.domain.model;

/** Wynik zapytania o cykl. Frontend zamienia każdy status na czytelny komunikat. */
public enum SeriesAnswerStatus {
    OK,
    /** model nie rozpoznał w tekście nazwy cyklu */
    SERIES_NAME_MISSING,
    /** żaden cykl w bibliotece nie pasuje do podanej nazwy */
    SERIES_NOT_FOUND,
    /** kilka cykli pasuje tak samo dobrze - użytkownik ma wybrać (patrz {@code options}) */
    AMBIGUOUS_SERIES,
    /** cykl istnieje, ale żaden z jego adresów nie jest obsługiwanym źródłem */
    NO_SUPPORTED_SOURCE,
    /** źródło nie odpowiedziało lub zwróciło pustą listę (np. zmienił się HTML) */
    SOURCE_UNAVAILABLE,
    /** cykl nie ma numerów tomów (np. seria wydawnicza), więc nie da się wskazać "następnego" */
    NOT_ORDERED,
    /** przeczytano już ostatni znany tom */
    NO_NEXT_VOLUME
}
