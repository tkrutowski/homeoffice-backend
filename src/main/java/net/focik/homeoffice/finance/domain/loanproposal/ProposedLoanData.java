package net.focik.homeoffice.finance.domain.loanproposal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Dane kredytu wyciągnięte przez Claude z treści e-maila - draft do wypełnienia formularza
 * dodawania kredytu na froncie. Celowo nie jest to domenowy {@code Loan}: pola są nullable
 * i luźno typowane (BigDecimal zamiast Money, bank jako surowy tekst), bo dopóki użytkownik
 * nie zatwierdzi propozycji, dane nie muszą spełniać reguł ważności prawdziwego kredytu.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class ProposedLoanData {
    /**
     * Adres e-mail oryginalnego nadawcy sprzed przekierowania (odczytany przez Claude z treści),
     * gdy wiadomość dotarła jako forward - w przeciwieństwie do {@code sourceEmailFrom} na
     * {@code LoanProposal}, który zawsze jest adresem osoby przekierowującej. Null, gdy nie da
     * się ustalić.
     */
    private String originalSenderEmail;
    /** Id istniejącego banku, jeśli udało się dopasować bankName do banku w bazie. */
    private Integer bankId;
    /** Nazwa banku/wierzyciela odczytana z maila (np. "PayPo") - zawsze wypełniona, niezależnie od dopasowania. */
    private String bankName;
    private String name;
    private BigDecimal amount;
    private LocalDate date;
    private String loanNumber;
    private String accountNumber;
    private LocalDate firstPaymentDate;
    private Integer numberOfInstallments;
    private BigDecimal installmentAmount;
    private BigDecimal loanCost;
    private String otherInfo;
}
