package net.focik.homeoffice.goahead.api.mapper;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.goahead.api.dto.AddressDto;
import net.focik.homeoffice.goahead.api.dto.LookupResponseDto;
import net.focik.homeoffice.goahead.domain.company.LookupResponse;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApiLookupMapper {

    public LookupResponseDto toDto(LookupResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getSubject() == null) {
            return null;
        }

        LookupResponse.Subject subject = response.getResult().getSubject();

        String firstAccountNumber = null;
        if (subject.getAccountNumbers() != null && !subject.getAccountNumbers().isEmpty()) {
            firstAccountNumber = subject.getAccountNumbers().get(0);
        }

        return LookupResponseDto.builder()
                .name(subject.getName())
                .nip(subject.getNip())
                .regon(subject.getRegon())
                .accountNumber(firstAccountNumber)
                .bankName(findBankName(firstAccountNumber))
                .addressDto(parseAddress(subject.getWorkingAddress()))
                .build();
    }

    private String findBankName(String firstAccountNumber) {
        if (firstAccountNumber == null || firstAccountNumber.trim().isEmpty()) {
            return null;
        }

        // Usuń spacje i zamień na wielkie litery dla IBAN
        String accountNumber = firstAccountNumber.replaceAll("\\s+", "").toUpperCase();

        // Normalizuj format numeru konta
        accountNumber = normalizeAccountNumber(accountNumber);

        // Sprawdź czy to prawidłowy polski IBAN po normalizacji
        if (!accountNumber.startsWith("PL") || accountNumber.length() < 8) {
            return "Nieprawidłowy format numeru konta";
        }

        // Wyciągnij kod banku z pozycji 5-8 (indeksy 4-7 w stringu)
        // PL 21 1030... -> pozycje 5-8 to 1030
        String bankCode;
        try {
            bankCode = accountNumber.substring(4, 8);
        } catch (StringIndexOutOfBoundsException e) {
            return "Nieprawidłowy format numeru konta";
        }

        // Mapa kodów banków na nazwy (główne polskie banki)
        return getBankNameByCode(bankCode);
    }

    private String normalizeAccountNumber(String accountNumber) {
        // Jeśli już zaczyna się od PL - zwróć bez zmian
        if (accountNumber.startsWith("PL")) {
            return accountNumber;
        }

        // Jeśli nie zaczyna się od PL, spróbuj dodać prefiks
        String digitsOnly = accountNumber.replaceAll("[^0-9]", "");

        if (digitsOnly.length() == 26) {
            // Pełne cyfry IBAN bez PL (26 cyfr) - dodaj PL
            return "PL" + digitsOnly;
        } else if (digitsOnly.length() >= 24 && digitsOnly.length() <= 25) {
            // Prawdopodobnie cyfry krajowe (24-25 cyfr) - dodaj PL i cyfry kontrolne
            // Dla uproszczenia używamy "00" jako cyfr kontrolnych
            return "PL00" + digitsOnly;
        }

        // Jeśli długość nie pasuje do żadnego formatu - zwróć oryginalny string
        // Metoda findBankName sprawdzi czy to prawidłowy format
        return accountNumber;
    }

    private String getBankNameByCode(String bankCode) {
        return switch (bankCode) {
            // 100-199
            case "1010" -> "Narodowy Bank Polski";
            case "1020" -> "Powszechna Kasa Oszczędności Bank Polski S.A.";
            case "1030" -> "Bank Handlowy w Warszawie S.A.";
            case "1050" -> "ING Bank Śląski S.A.";
            case "1060" -> "Bank BPH S.A.";
            case "1090" -> "Santander Bank Polska S.A.";
            case "1130" -> "Bank Gospodarstwa Krajowego";
            case "1140" -> "mBank S.A.";
            case "1160" -> "Bank Millennium S.A.";
            case "1240" -> "Bank Polska Kasa Opieki S.A.";
            case "1280" -> "HSBC Bank Polska S.A.";
            case "1320" -> "Bank Pocztowy S.A.";
            case "1540" -> "Bank Ochrony Środowiska S.A.";
            case "1610" -> "SGB-Bank S.A.";
            case "1680" -> "PLUS BANK S.A.";
            case "1840" -> "Societe Generale S.A. Oddział w Polsce";
            case "1870" -> "Nest Bank S.A.";
            case "1930" -> "Bank Polskiej Spółdzielczości S.A.";
            case "1940" -> "Credit Agricole Bank Polska S.A.";
            case "1950" -> "Idea Bank S.A.";
            // 200-299
            case "2030" -> "BNP Paribas Bank Polska S.A.";
            case "2120" -> "Santander Consumer Bank S.A.";
            case "2130" -> "Volkswagen Bank Polska S.A.";
            case "2140" -> "Toyota Bank Polska S.A.";
            case "2160" -> "mBank Hipoteczny S.A.";
            case "2190" -> "DNB Bank Polska S.A.";
            case "2350" -> "BNP Paribas S.A. Oddział w Polsce";
            case "2360" -> "Danske Bank A/S S.A. Oddział w Polsce";
            case "2370" -> "Skandinaviska Enskilda Banken AB (S.A.) Oddział w Polsce";
            case "2390" -> "CAIXABANK, S.A. (SPÓŁKA AKCYJNA) ODDZIAŁ W POLSCE";
            case "2410" -> "U.S. Bank Europe Designated Activity Company Oddział w Polsce";
            case "2470" -> "HAITONG BANK, S.A. Spółka Akcyjna Oddział w Polsce";
            case "2490" -> "Alior Bank S.A.";
            case "2510" -> "Aareal Bank Aktiengesellschaft S.A. Oddział w Polsce";
            case "2540" -> "Citibank Europe plc (Publiczna Spółka Akcyjna) Oddział w Polsce";
            case "2550" -> "Ikano Bank AB (publ) Spółka Akcyjna Oddział w Polsce";
            case "2560" -> "Nordea Bank Abp S.A. Oddział w Polsce";
            case "2600" -> "Bank of China (Europe) S.A. Spółka Akcyjna Oddział w Polsce";
            case "2620" -> "Industrial and Commercial Bank of China (Europe) S.A. Oddział w Polsce";
            case "2640" -> "RCI Banque Spółka Akcyjna Oddział w Polsce";
            case "2650" -> "EUROCLEAR Bank SA/NV (Spółka Akcyjna) Oddział w Polsce";
            case "2660" -> "Intesa Sanpaolo S.p.A. Spółka Akcyjna Oddział w Polsce";
            case "2670" -> "Western Union International Bank GmbH, Sp. z o.o. Oddział w Polsce";
            case "2690" -> "PKO Bank Hipoteczny S.A.";
            case "2700" -> "TF BANK AB (Spółka Akcyjna) Oddział w Polsce";
            case "2720" -> "AS Inbank Spółka Akcyjna Oddział w Polsce";
            case "2730" -> "China Construction Bank (Europe) S.A. Spółka Akcyjna Oddział w Polsce";
            case "2750" -> "John Deere Bank S.A. Spółka Akcyjna Oddział w Polsce";
            case "2770" -> "Volkswagen Bank GmbH Sp. z o.o. Oddział w Polsce";
            case "2780" -> "ING Bank Hipoteczny S.A.";
            case "2790" -> "Raiffeisen Bank International AG (Spółka Akcyjna) Oddział w Polsce";
            case "2800" -> "HSBC Continental Europe (Spółka Akcyjna) Oddział w Polsce";
            case "2810" -> "Goldman Sachs Bank Europe SE Spółka Europejska Oddział w Polsce";
            case "2830" -> "J.P. Morgan SE (Spółka Europejska) Oddział w Polsce";
            case "2850" -> "BFF Bank S.p.A. Spółka Akcyjna Oddział w Polsce";
            case "2860" -> "CA Auto Bank S.p.A. Spółka Akcyjna Oddział w Polsce";
            case "2870" -> "Bank Nowy Spółka Akcyjna";
            case "2880" -> "Allfunds Bank S.A.U. (Spółka Akcyjna) Oddział w Polsce";
            case "2890" -> "Hoist Finance AB (publ) Spółka Akcyjna Oddział w Polsce";
            case "2900" -> "Millennium Bank Hipoteczny S.A.";
            case "2910" -> "UniCredit S.A. Spółka Akcyjna Oddział w Polsce";
            case "2930" -> "VeloBank Spółka Akcyjna";
            case "2950" -> "KEB Hana Bank (D) AG Spółka Akcyjna Oddział w Polsce";
            case "2960" -> "Woori Bank Europe GmbH spółka z ograniczoną odpowiedzialnością Oddział w Polsce";
            case "2970" -> "IBK Bank Polska S.A.";
            case "2980" -> "Trade Republic Bank GmbH spółka z ograniczoną odpowiedzialnością Oddział w Polsce";
            // domyślna odpowiedź
            default -> "Nieznany bank (kod: " + bankCode + ")";
        };
    }

    private AddressDto parseAddress(String addressString) {
        if (addressString == null || addressString.isBlank()) {
            return null;
        }

        // MF API usually returns addresses like: "ul. Kwiatowa 1, 00-001 Warszawa"
        // or "Kwiatowa 1, 00-001 Warszawa" or "Kwiatowa 1 lok. 2, 00-001 Warszawa"
        AddressDto addressDto = new AddressDto();

        try {
            int lastCommaIndex = addressString.lastIndexOf(',');
            if (lastCommaIndex != -1) {
                String street = addressString.substring(0, lastCommaIndex).trim();
                String cityAndZip = addressString.substring(lastCommaIndex + 1).trim();

                String[] cityParts = cityAndZip.split(" ", 2);
                if (cityParts.length == 2) {
                    addressDto.setZip(cityParts[0]);
                    addressDto.setCity(cityParts[1]);
                } else {
                    addressDto.setCity(cityAndZip);
                }
                addressDto.setStreet(street);
            } else {
                addressDto.setStreet(addressString);
            }
        } catch (Exception e) {
            addressDto.setStreet(addressString); // Fallback to assigning everything to street
        }

        return addressDto;
    }
}
