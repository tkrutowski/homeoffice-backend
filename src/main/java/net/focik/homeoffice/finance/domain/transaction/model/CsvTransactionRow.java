package net.focik.homeoffice.finance.domain.transaction.model;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class CsvTransactionRow {
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
    private String type;
    private String categoryName;
    private BigDecimal amount;
    private String note;
    private String labels;
    private int rowNumber;
}
