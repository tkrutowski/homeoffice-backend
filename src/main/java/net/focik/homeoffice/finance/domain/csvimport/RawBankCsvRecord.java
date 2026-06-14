package net.focik.homeoffice.finance.domain.csvimport;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class RawBankCsvRecord {
    private String accountNumber;      // Nr rachunku/karty
    private LocalDate transactionDate; // Data transakcji
    private String transactionType;    // Rodzaj transakcji (dla kont PL)
    private String recipientSender;    // Odbiorca/Zleceniodawca
    private String description;        // Opis
    private BigDecimal debit;          // Obciążenia
    private BigDecimal credit;         // Uznania
    private BigDecimal balance;        // Saldo
    private String currency;           // Waluta
    private int rowNumber;             // Numer wiersza w pliku (dla błędów)

    public boolean isAccountRow() {
        return accountNumber != null && accountNumber.startsWith("PL");
    }

    public String getLastFourDigits() {
        if (accountNumber == null) return "";
        String cleaned = accountNumber.replaceAll("\\s+", "");
        return cleaned.length() >= 4 ? cleaned.substring(cleaned.length() - 4) : cleaned;
    }
}
