package net.focik.homeoffice.library.domain;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;

/** Normalizacja tekstu do porównywania tytułów, autorów i nazw cykli. */
final class SearchText {

    private SearchText() {
    }

    /** Małe litery, bez znaków diakrytycznych (ł nie rozkłada się w NFD, więc osobno), jedna spacja między słowami. */
    static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(text.toLowerCase().replace('ł', 'l'), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "").replaceAll("\\s+", " ").trim();
    }

    /** Jak {@link #normalize}, ale dodatkowo bez interpunkcji ("Jack Reacher. Zasady" == "jack reacher zasady"). */
    static String loose(String text) {
        return normalize(text).replaceAll("[^a-z0-9]+", " ").trim();
    }

    /** Słowa znormalizowane jak w {@link #loose}; jednoliterowe litery ("z", "o") odpadają, cyfry zostają ("Part 2"). */
    static List<String> tokens(String text) {
        return Arrays.stream(loose(text).split(" "))
                .filter(t -> t.length() > 1 || (t.length() == 1 && Character.isDigit(t.charAt(0))))
                .toList();
    }
}
