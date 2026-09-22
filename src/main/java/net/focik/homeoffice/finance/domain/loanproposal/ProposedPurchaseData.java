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
    /**
     * Kopia {@code LoanProposal.idUser} (dopasowanego po nadawcy maila przy ingest, zob.
     * {@code LoanProposalFacade.resolveIdUserFromEmail}) doklejana przez
     * {@code LoanProposalExtractionRunner} po ekstrakcji - żeby front mógł od razu podpowiedzieć
     * usera w formularzu zakupu, bez sięgania po {@code LoanProposalDto.idUser} osobno. Null, gdy
     * dopasowanie nadawcy się nie powiodło - wtedy usera trzeba wybrać ręcznie.
     */
    private Integer idUser;
    /**
     * Dopasowana po nazwie karta (np. "Allegro Pay", "PayPo") - szukana w tytule maila, nie w
     * treści, bo to tam typowo pojawia się nazwa metody płatności ("Potwierdzenie płatności kartą
     * Allegro Pay"). Null, gdy żadna aktywna karta nie pasuje - wtedy trzeba wybrać ręcznie.
     */
    private Integer idCard;
    /**
     * Dopasowana po nazwie firma - najpierw szukana w {@code name} (merchantName z treści maila,
     * np. "JMP S.A. BIEDRONKA" -> firma "Biedronka"), a dopiero gdy to zawiedzie, w tytule maila
     * tak jak {@code idCard}. Null, gdy żadne z tych dopasowań się nie powiedzie.
     */
    private Integer idFirm;
}
