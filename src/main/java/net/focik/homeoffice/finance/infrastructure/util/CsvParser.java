package net.focik.homeoffice.finance.infrastructure.util;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.finance.domain.transaction.model.CsvTransactionRow;
import net.focik.homeoffice.finance.domain.transaction.TransactionImportService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CsvParser {

    private final TransactionImportService transactionImportService;

    public List<CsvTransactionRow> parseCsv(byte[] fileContent) throws IOException {
        List<CsvTransactionRow> rows = new ArrayList<>();

        Charset charset = detectCharset(fileContent);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(fileContent), charset))) {

            CSVFormat format = CSVFormat.DEFAULT
                    .withDelimiter(';')
                    .withFirstRecordAsHeader()
                    .withTrim();

            try (CSVParser csvParser = new CSVParser(reader, format)) {
                int rowNumber = 2;
                for (CSVRecord record : csvParser) {
                    try {
                        CsvTransactionRow row = CsvTransactionRow.builder()
                                .rowNumber(rowNumber)
                                .date(parseDate(record.get("Date")))
                                .type(record.get("Type"))
                                .categoryName(record.get("Category name"))
                                .amount(parseAmount(record.get("Amount")))
                                .note(record.get("Note"))
                                .labels(record.get("Labels"))
                                .build();
                        rows.add(row);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Błąd w wierszu " + rowNumber + ": " + e.getMessage());
                    }
                    rowNumber++;
                }
            }
        }

        return rows;
    }

    private LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.isBlank()) {
            throw new IllegalArgumentException("Data nie może być pusta");
        }
        return transactionImportService.parseDate(dateString);
    }

    private BigDecimal parseAmount(String amountString) {
        if (amountString == null || amountString.isBlank()) {
            throw new IllegalArgumentException("Kwota nie może być pusta");
        }
        return transactionImportService.parseAmount(amountString);
    }

    private Charset detectCharset(byte[] content) {
        if (content.length >= 3) {
            if (content[0] == (byte) 0xEF && content[1] == (byte) 0xBB && content[2] == (byte) 0xBF) {
                return StandardCharsets.UTF_8;
            }
        }
        if (content.length >= 2) {
            if (content[0] == (byte) 0xFF && content[1] == (byte) 0xFE) {
                return StandardCharsets.UTF_16LE;
            }
            if (content[0] == (byte) 0xFE && content[1] == (byte) 0xFF) {
                return StandardCharsets.UTF_16BE;
            }
        }

        Charset[] charsetsToTry = {
            Charset.forName("Windows-1250"),
            StandardCharsets.UTF_8,
            Charset.forName("ISO-8859-2")
        };

        for (Charset charset : charsetsToTry) {
            try {
                new String(content, charset);
                return charset;
            } catch (Exception e) {
                // Try next charset
            }
        }

        return Charset.forName("Windows-1250");
    }
}
