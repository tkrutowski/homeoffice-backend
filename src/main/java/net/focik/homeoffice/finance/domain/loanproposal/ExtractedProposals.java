package net.focik.homeoffice.finance.domain.loanproposal;

import java.util.Optional;

/**
 * Wynik {@link LoanProposalExtractionService#extract(String)} - z jednego e-maila może powstać
 * kandydat na propozycję kredytu, propozycję zakupu, oba naraz (typowo PayPo/Allegro - zakup
 * sfinansowany kredytem ratalnym) albo żaden (e-mail nierozpoznany jako dokument kredytowy).
 */
public record ExtractedProposals(Optional<ProposedLoanData> loan, Optional<ProposedPurchaseData> purchase) {

    static ExtractedProposals none() {
        return new ExtractedProposals(Optional.empty(), Optional.empty());
    }

    static ExtractedProposals of(ProposedLoanData loan, ProposedPurchaseData purchase) {
        return new ExtractedProposals(Optional.of(loan), Optional.ofNullable(purchase));
    }

    public boolean isEmpty() {
        return loan.isEmpty() && purchase.isEmpty();
    }
}
