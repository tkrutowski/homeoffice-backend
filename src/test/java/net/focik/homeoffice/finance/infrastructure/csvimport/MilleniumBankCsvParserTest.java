package net.focik.homeoffice.finance.infrastructure.csvimport;

import net.focik.homeoffice.finance.domain.csvimport.RawBankCsvRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MilleniumBankCsvParserTest {

    @InjectMocks
    private MilleniumBankCsvParser csvParser;

    @Test
    @DisplayName("should parse an account row correctly when the account CSV data is valid")
    void parseCsv_ShouldParseAccountRowCorrectly_WhenValidAccountData() {
        String csvContent = "Numer rachunku/karty,Data transakcji,Data rozliczenia,Rodzaj transakcji,Na konto/Z konta,Odbiorca/Zleceniodawca,Opis,Obciążenia,Uznania,Saldo,Waluta\n" +
                "\"PL77 1160 2202 0000 0002 6727 6225\",\"2026-06-12\",\"2026-06-12\",\"PRZELEW NA TELEFON\",\"19 11 6022 0200 0000 0362 2978 87\",\"Stasiu\",\"Przelew BLIK na telefon\",\"-15.00\",\"\",\"-3237.82\",\"PLN\"";

        MilleniumBankCsvParser.CsvParseResult result = csvParser.parseCsv(csvContent.getBytes(StandardCharsets.UTF_8));

        assertThat(result.records).hasSize(1);
        assertThat(result.errors).isEmpty();

        RawBankCsvRecord record = result.records.getFirst();
        assertThat(record.getAccountNumber()).isEqualTo("PL77 1160 2202 0000 0002 6727 6225");
        assertThat(record.getTransactionDate()).isEqualTo(LocalDate.of(2026, 6, 12));
        assertThat(record.getTransactionType()).isEqualTo("PRZELEW NA TELEFON");
        assertThat(record.getRecipientSender()).isEqualTo("Stasiu");
        assertThat(record.getDescription()).isEqualTo("Przelew BLIK na telefon");
        assertThat(record.getDebit()).isEqualTo(new BigDecimal("-15.00"));  // parser zwraca oryginalną wartość z CSV
        assertThat(record.getCredit()).isNull();
        assertThat(record.getBalance()).isEqualTo(new BigDecimal("-3237.82"));
        assertThat(record.getCurrency()).isEqualTo("PLN");
        assertThat(record.isAccountRow()).isTrue();
    }

    @Test
    @DisplayName("should parse a card purchase row correctly when the card CSV data is valid")
    void parseCsv_ShouldParsePurchaseRowCorrectly_WhenValidCardData() {
        String csvContent = "Numer rachunku/karty,Data transakcji,Data rozliczenia,Rodzaj transakcji,Na konto/Z konta,Odbiorca/Zleceniodawca,Opis,Obciążenia,Uznania,Saldo,Waluta\n" +
                "\"4603 XXXX XXXX 5473\",\"2026-06-01\",\"2026-06-03\",\"\",\"\",\"\",\"Pierogarnia RECZNIE LEPI\",\"-27.0\",\"\",\"\",\"PLN\"";

        MilleniumBankCsvParser.CsvParseResult result = csvParser.parseCsv(csvContent.getBytes(StandardCharsets.UTF_8));

        assertThat(result.records).hasSize(1);
        assertThat(result.errors).isEmpty();

        RawBankCsvRecord record = result.records.getFirst();
        assertThat(record.getAccountNumber()).isEqualTo("4603 XXXX XXXX 5473");
        assertThat(record.getTransactionDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(record.getDebit()).isEqualTo(new BigDecimal("-27.0"));
        assertThat(record.getCredit()).isNull();
        assertThat(record.isAccountRow()).isFalse();
    }

    @Test
    @DisplayName("should record a credit-only row with a null debit when there is credit but no debit")
    void parseCsv_ShouldSkipCreditOnlyRows_WhenCreditWithoutDebit() {
        String csvContent = "Numer rachunku/karty,Data transakcji,Data rozliczenia,Rodzaj transakcji,Na konto/Z konta,Odbiorca/Zleceniodawca,Opis,Obciążenia,Uznania,Saldo,Waluta\n" +
                "\"4603 XXXX XXXX 5473\",\"2026-06-06\",\"2026-06-08\",\"\",\"\",\"\",\"Douglas Polska\",\"\",\"2.3\",\"\",\"PLN\"";

        MilleniumBankCsvParser.CsvParseResult result = csvParser.parseCsv(csvContent.getBytes(StandardCharsets.UTF_8));

        assertThat(result.records).hasSize(1);
        RawBankCsvRecord record = result.records.getFirst();
        assertThat(record.getDebit()).isNull();
        assertThat(record.getCredit()).isEqualTo(new BigDecimal("2.3"));
    }

    @Test
    @DisplayName("should parse amounts correctly regardless of decimal separator")
    void parseCsv_ShouldParseAmountsWithCommaDecimalSeparator() {
        String csvContent = "Numer rachunku/karty,Data transakcji,Data rozliczenia,Rodzaj transakcji,Na konto/Z konta,Odbiorca/Zleceniodawca,Opis,Obciążenia,Uznania,Saldo,Waluta\n" +
                "\"4603 XXXX XXXX 5473\",\"2026-06-04\",\"2026-06-06\",\"\",\"\",\"\",\"OPERA DELLA PRIMAZIALE\",\"-229.3\",\"\",\"\",\"PLN\"";

        MilleniumBankCsvParser.CsvParseResult result = csvParser.parseCsv(csvContent.getBytes(StandardCharsets.UTF_8));

        assertThat(result.records).hasSize(1);
        RawBankCsvRecord record = result.records.getFirst();
        assertThat(record.getDebit()).isEqualTo(new BigDecimal("-229.3"));
    }

    @Test
    @DisplayName("should extract the last four digits when the card number is masked with XXXX")
    void parseCsv_ShouldGetLastFourDigits_WhenCardNumberHasXXXX() {
        String csvContent = "Numer rachunku/karty,Data transakcji,Data rozliczenia,Rodzaj transakcji,Na konto/Z konta,Odbiorca/Zleceniodawca,Opis,Obciążenia,Uznania,Saldo,Waluta\n" +
                "\"4603 XXXX XXXX 5473\",\"2026-06-01\",\"2026-06-03\",\"\",\"\",\"\",\"Test\",\"-1.0\",\"\",\"\",\"PLN\"";

        MilleniumBankCsvParser.CsvParseResult result = csvParser.parseCsv(csvContent.getBytes(StandardCharsets.UTF_8));

        assertThat(result.records).hasSize(1);
        RawBankCsvRecord record = result.records.getFirst();
        assertThat(record.getLastFourDigits()).isEqualTo("5473");
    }

    @Test
    @DisplayName("should report an error and skip the record when the date format cannot be parsed")
    void parseCsv_ShouldHandleInvalidDateFormat_WhenDateIsNotParseable() {
        String csvContent = "Numer rachunku/karty,Data transakcji,Data rozliczenia,Rodzaj transakcji,Na konto/Z konta,Odbiorca/Zleceniodawca,Opis,Obciążenia,Uznania,Saldo,Waluta\n" +
                "\"4603 XXXX XXXX 5473\",\"invalid-date\",\"2026-06-03\",\"\",\"\",\"\",\"Test\",\"-1.0\",\"\",\"\",\"PLN\"";

        MilleniumBankCsvParser.CsvParseResult result = csvParser.parseCsv(csvContent.getBytes(StandardCharsets.UTF_8));

        assertThat(result.records).isEmpty();
        assertThat(result.errors).isNotEmpty();
        assertThat(result.errors.getFirst()).contains("Niepoprawny format daty");
    }

    @Test
    @DisplayName("should ignore the header row and parse only the data rows when the CSV contains a header")
    void parseCsv_ShouldIgnoreHeaderRow_WhenCSVContainsHeader() {
        String csvContent = "Numer rachunku/karty,Data transakcji,Data rozliczenia,Rodzaj transakcji,Na konto/Z konta,Odbiorca/Zleceniodawca,Opis,Obciążenia,Uznania,Saldo,Waluta\n" +
                "\"4603 XXXX XXXX 5473\",\"2026-06-01\",\"2026-06-03\",\"\",\"\",\"\",\"Test\",\"-1.0\",\"\",\"\",\"PLN\"\n" +
                "\"4603 XXXX XXXX 5473\",\"2026-06-02\",\"2026-06-04\",\"\",\"\",\"\",\"Test2\",\"-2.0\",\"\",\"\",\"PLN\"";

        MilleniumBankCsvParser.CsvParseResult result = csvParser.parseCsv(csvContent.getBytes(StandardCharsets.UTF_8));

        assertThat(result.records).hasSize(2);
    }

    @Test
    @DisplayName("should return no records and no errors when the file contains only the header")
    void parseCsv_ShouldHandleEmptyFile_WhenNoRecordsPresent() {
        String csvContent = "Numer rachunku/karty,Data transakcji,Data rozliczenia,Rodzaj transakcji,Na konto/Z konta,Odbiorca/Zleceniodawca,Opis,Obciążenia,Uznania,Saldo,Waluta";

        MilleniumBankCsvParser.CsvParseResult result = csvParser.parseCsv(csvContent.getBytes(StandardCharsets.UTF_8));

        assertThat(result.records).isEmpty();
        assertThat(result.errors).isEmpty();
    }
}
