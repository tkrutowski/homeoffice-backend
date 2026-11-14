package net.focik.homeoffice.library.domain.model;

/** Status tomu cyklu z punktu widzenia biblioteki użytkownika. */
public enum SeriesVolumeStatus {
    /** przeczytany (którekolwiek wydanie) */
    READ,
    /** aktualnie czytany */
    READING,
    /** jest na półce, ale nieprzeczytany */
    ON_SHELF,
    /** nie ma w bibliotece użytkownika */
    NOT_IN_LIBRARY
}
