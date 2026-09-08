package net.focik.homeoffice.finance.domain.loanproposal;

/**
 * Cykl życia propozycji kredytu wygenerowanej z przychodzącego e-maila.
 * <p>
 * NEW -> (EXTRACTED | FAILED) -> (ACCEPTED | IGNORED)
 */
public enum LoanProposalStatus {
    /** Zapisana z n8n, czeka na ekstrakcję danych przez Claude. */
    NEW,
    /** Claude rozpoznał dokument jako kredyt i wypełnił {@link ProposedLoanData}. */
    EXTRACTED,
    /** Claude nie rozpoznał kredytu albo ekstrakcja się nie powiodła (patrz failureReason). */
    FAILED,
    /** Użytkownik zatwierdził propozycję - powstał realny Loan (createdLoanId ustawiony). */
    ACCEPTED,
    /** Użytkownik odrzucił propozycję. */
    IGNORED
}
