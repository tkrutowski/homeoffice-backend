package net.focik.homeoffice.finance.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;

/**
 * Draft danych kredytu do wypełnienia formularza dodawania kredytu na froncie - pola nazwane
 * tak samo jak w {@link LoanDto}, ale wszystkie nullable, bo to tylko propozycja.
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@ToString
public class ProposedLoanDataDto {
    /** Adres e-mail oryginalnego nadawcy sprzed przekierowania (patrz ProposedLoanData), lub null. */
    private String originalSenderEmail;
    private Integer bankId;
    private String bankName;
    private String name;
    private Number amount;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Europe/Warsaw")
    private LocalDate date;
    private String loanNumber;
    private String accountNumber;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Europe/Warsaw")
    private LocalDate firstPaymentDate;
    private Integer numberOfInstallments;
    private Number installmentAmount;
    private Number loanCost;
    private String otherInfo;
}
