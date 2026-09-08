package net.focik.homeoffice.finance.domain.loanproposal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Surowa odpowiedź Claude na temat treści maila - zanim zostanie zmapowana na
 * {@link net.focik.homeoffice.finance.domain.loanproposal.ProposedLoanData} (m.in. dopasowanie
 * banku po nazwie). Wszystkie pola oprócz {@code isLoanDocument} są tekstowe/nullable,
 * bo tak przychodzą z modelu - parsowanie liczb/dat dzieje się wyżej, tak jak w
 * {@code ClaudeInvoiceParserService}.
 */
@Builder
@AllArgsConstructor
@Getter
@ToString
public class LoanExtractionResult {
    private boolean isLoanDocument;
    /**
     * Adres e-mail faktycznego nadawcy dokumentu (np. noreply@paypo.pl), odczytany z treści -
     * gdy wiadomość jest przekierowana (np. z prywatnej skrzynki), nagłówek From całego maila
     * to zawsze adres przekierowującego, nie oryginalnego nadawcy. Null, gdy nie da się ustalić
     * (wiadomość niebędąca przekierowaniem, albo brak nagłówków w treści).
     */
    private String originalSenderEmail;
    private String bankOrCreditor;
    /**
     * Nazwa sklepu/tytuł zakupu (np. "GLOBAL-E.SHELLY EU" z "zamówienie w sklepie X") - to co
     * zostało kupione, w odróżnieniu od {@code bankOrCreditor}, czyli kto sfinansował zakup.
     * Null, gdy e-mail nie dotyczy konkretnego zakupu (np. zwykły kredyt gotówkowy/hipoteczny).
     */
    private String merchantName;
    private String loanNumber;
    /** Numer rachunku bankowego do przelewu (np. z sekcji "tradycyjny przelew"), lub null. */
    private String accountNumber;
    private String amount;
    private String installmentAmount;
    private Integer numberOfInstallments;
    private String firstPaymentDate;
    private String loanCost;
    private String otherInfo;
}
