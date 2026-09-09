package net.focik.homeoffice.finance.domain.loanproposal.port.primary;

import net.focik.homeoffice.finance.domain.purchase.Purchase;

public interface AcceptLoanProposalAsPurchaseUseCase {
    /**
     * Zatwierdza propozycję jako zakup (alternatywa dla {@link AcceptLoanProposalUseCase#accept}):
     * {@code finalPurchase} (dane z formularza, ewentualnie poprawione przez użytkownika) trafiają
     * do tego samego {@code AddPurchaseUseCase.addPurchase(...)}, którego używa zwykłe ręczne
     * dodawanie zakupu. Propozycja dostaje status ACCEPTED i createdPurchaseId.
     */
    Purchase acceptAsPurchase(int proposalId, Purchase finalPurchase);
}
