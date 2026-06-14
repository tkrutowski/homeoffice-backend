package net.focik.homeoffice.finance.infrastructure.csvimport;

import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.finance.domain.csvimport.RawBankCsvRecord;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class MilleniumBankCsvParser {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String[] EXPECTED_HEADERS = {
            "Numer rachunku/karty",
            "Data transakcji",
            "Data rozliczenia",
            "Rodzaj transakcji",
            "Na konto/Z konta",
            "Odbiorca/Zleceniodawca",
            "Opis",
            "Obciążenia",
            "Uznania",
            "Saldo",
            "Waluta"
    };

    public CsvParseResult parseCsv(byte[] fileContent) {
        List<RawBankCsvRecord> records = new ArrayList<>();
        List<String> parseErrors = new ArrayList<>();

        try {
            Charset charset = detectCharset(fileContent);
            String csvContent = new String(fileContent, charset);
            csvContent = preprocessCsv(csvContent);

            CSVFormat format = CSVFormat.DEFAULT.withFirstRecordAsHeader();
            try (InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(csvContent.getBytes(charset)), charset);
                 CSVParser csvParser = format.parse(reader)) {

                int rowNumber = 1;
                for (CSVRecord record : csvParser) {
                    rowNumber++;
                    try {
                        RawBankCsvRecord parsedRecord = parseCsvRow(record, rowNumber);
                        if (parsedRecord != null) {
                            records.add(parsedRecord);
                        }
                    } catch (Exception e) {
                        parseErrors.add("Wiersz " + rowNumber + ": " + e.getMessage());
                        log.debug("Error parsing row {}: {}", rowNumber, e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            parseErrors.add("Błąd odczytu pliku CSV: " + e.getMessage());
            log.error("Error reading CSV file", e);
        }

        return new CsvParseResult(records, parseErrors);
    }

    private RawBankCsvRecord parseCsvRow(CSVRecord record, int rowNumber) {
        String accountNumber = getColumnValue(record, 0);
        String transactionDateStr = getColumnValue(record, 1);
        String transactionType = getColumnValue(record, 3);
        String recipientSender = getColumnValue(record, 5);
        String description = getColumnValue(record, 6);
        String debitStr = getColumnValue(record, 7);
        String creditStr = getColumnValue(record, 8);
        String balanceStr = getColumnValue(record, 9);
        String currency = getColumnValue(record, 10);

        if (accountNumber.isEmpty()) {
            return null;
        }

        LocalDate transactionDate = parseDate(transactionDateStr);
        if (transactionDate == null) {
            throw new IllegalArgumentException("Niepoprawny format daty: " + transactionDateStr);
        }

        BigDecimal debit = parseBigDecimal(debitStr);
        BigDecimal credit = parseBigDecimal(creditStr);
        BigDecimal balance = parseBigDecimal(balanceStr);

        return RawBankCsvRecord.builder()
                .accountNumber(accountNumber)
                .transactionDate(transactionDate)
                .transactionType(transactionType)
                .recipientSender(recipientSender)
                .description(description)
                .debit(debit)
                .credit(credit)
                .balance(balance)
                .currency(currency)
                .rowNumber(rowNumber)
                .build();
    }

    private String getColumnValue(CSVRecord record, int index) {
        try {
            if (index < record.size()) {
                String value = record.get(index);
                return value == null ? "" : value.trim();
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        try {
            String cleaned = value.trim()
                    .replace("\"", "")
                    .replace(" ", "");
            if (cleaned.isEmpty()) {
                return null;
            }
            cleaned = cleaned.replace(",", ".");
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String preprocessCsv(String csvContent) {
        String[] lines = csvContent.split("\n", -1);
        StringBuilder result = new StringBuilder();

        for (int lineNum = 0; lineNum < lines.length; lineNum++) {
            if (lineNum > 0) {
                result.append("\n");
            }

            String line = lines[lineNum];
            result.append(fixLineQuotes(line));
        }

        return result.toString();
    }

    private String fixLineQuotes(String line) {
        StringBuilder result = new StringBuilder();
        int fieldStart = 0;
        boolean inQuotes = false;

        for (int i = 0; i <= line.length(); i++) {
            char c = i < line.length() ? line.charAt(i) : ',';

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                String field = line.substring(fieldStart, i);
                field = fixField(field);

                result.append(field);
                if (i < line.length()) {
                    result.append(",");
                }
                fieldStart = i + 1;
            }
        }

        return result.toString();
    }

    private String fixField(String field) {
        if (field.startsWith("\"\"") && field.endsWith("\"") && field.length() > 2) {
            String content = field.substring(2, field.length() - 1);

            if (content.contains("\"")) {
                content = content.replace("\"", "");
                return "\"" + content + "\"";
            }
        }

        return field;
    }

    private Charset detectCharset(byte[] fileContent) {
        if (fileContent.length >= 3) {
            if (fileContent[0] == (byte) 0xEF &&
                    fileContent[1] == (byte) 0xBB &&
                    fileContent[2] == (byte) 0xBF) {
                return StandardCharsets.UTF_8;
            }
        }

        if (fileContent.length >= 2) {
            if (fileContent[0] == (byte) 0xFF && fileContent[1] == (byte) 0xFE) {
                return StandardCharsets.UTF_16LE;
            }
            if (fileContent[0] == (byte) 0xFE && fileContent[1] == (byte) 0xFF) {
                return StandardCharsets.UTF_16BE;
            }
        }

        return StandardCharsets.UTF_8;
    }

    public static class CsvParseResult {
        public final List<RawBankCsvRecord> records;
        public final List<String> errors;

        public CsvParseResult(List<RawBankCsvRecord> records, List<String> errors) {
            this.records = records;
            this.errors = errors;
        }
    }
}
