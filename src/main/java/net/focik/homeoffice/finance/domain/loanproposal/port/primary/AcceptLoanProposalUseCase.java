package net.focik.homeoffice.finance.domain.loanproposal.port.primary;

import net.focik.homeoffice.finance.domain.loan.Loan;

public interface AcceptLoanProposalUseCase {
    /**
     * Zatwierdza propozycję: {@code finalLoan} (dane z formularza, ewentualnie poprawione przez
     * użytkownika) trafiają do tego samego {@code AddLoanUseCase.addLoan(...)}, którego używa
     * zwykłe ręczne dodawanie kredytu. Propozycja dostaje status ACCEPTED i createdLoanId.
     */
    Loan accept(int proposalId, Loan finalLoan);
}
