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
 * Dane zakupu wyciągnięte z tej samej ekstrakcji co {@link ProposedLoanData} - draft do
 * wypełnienia formularza dodawania zakupu na froncie, gdy e-mail dotyczy zakupu sfinansowanego
 * kredytem (np. PayPo, Allegro), a nie zwykłego kredytu bankowego. Celowo bez {@code paymentDeadline}
 * - ten termin zależy od karty wybranej przez użytkownika w formularzu i tak czy inaczej zostaje
 * przeliczony po stronie serwera (patrz {@code PurchaseService.addPurchase}), więc nie ma sensu
 * proponować go na etapie ekstrakcji.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class ProposedPurchaseData {
    /** CO zostało kupione (merchantName z ekstrakcji), np. "GLOBAL-E.SHELLY EU". */
    private String name;
    private BigDecimal amount;
    private LocalDate purchaseDate;
    private String otherInfo;
    /** Sugestia Claude, czy zakup jest ratalny (numberOfInstallments > 1) - informacyjne, front może zignorować. */
    private boolean installment;
}
