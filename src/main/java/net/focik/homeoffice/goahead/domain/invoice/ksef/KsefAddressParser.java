package net.focik.homeoffice.goahead.domain.invoice.ksef;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rozbija adres z KSeF (AdresL1/AdresL2) na ulicę, kod pocztowy i miasto.
 */
public final class KsefAddressParser {

    private static final Pattern ZIP_CODE_PATTERN = Pattern.compile("(\\d{2}-\\d{3})");

    public record ParsedAddress(String city, String street, String zip) {}

    private KsefAddressParser() {
    }

    public static Optional<ParsedAddress> parse(String adresL1, String adresL2) {
        if (adresL1 == null || adresL1.isBlank()) {
            return Optional.empty();
        }

        String street = adresL1.trim();
        String city = null;
        String zip = null;

        Matcher zipMatcher = ZIP_CODE_PATTERN.matcher(street);
        if (zipMatcher.find()) {
            zip = zipMatcher.group(1);
            int zipStart = zipMatcher.start();
            city = street.substring(zipMatcher.end()).trim();
            if (city.isEmpty()) {
                city = null;
            }
            street = street.substring(0, zipStart).trim();
        } else if (adresL2 != null && !adresL2.isBlank()) {
            Matcher l2Matcher = ZIP_CODE_PATTERN.matcher(adresL2);
            if (l2Matcher.find()) {
                zip = l2Matcher.group(1);
                String remaining = adresL2.substring(l2Matcher.end()).trim();
                if (!remaining.isEmpty()) {
                    city = remaining;
                }
            }
        }

        return Optional.of(new ParsedAddress(city, removeStreetPrefix(street), zip));
    }

    private static String removeStreetPrefix(String street) {
        if (street == null || street.isBlank()) {
            return street;
        }
        return street.replaceAll("(?i)^(ul\\.?|ulica)\\s+", "").trim();
    }
}
