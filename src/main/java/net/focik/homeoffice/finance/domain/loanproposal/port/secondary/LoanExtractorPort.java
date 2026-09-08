package net.focik.homeoffice.finance.domain.loanproposal.port.secondary;

import net.focik.homeoffice.finance.domain.loanproposal.LoanExtractionResult;

public interface LoanExtractorPort {
    /**
     * Wysyła tekst maila do Claude z wymuszonym schematem JSON i zwraca sparsowaną odpowiedź.
     * Nie rzuca wyjątku dla "nie rozpoznano kredytu" - zwraca wynik z isLoanDocument=false.
     * Rzuca wyjątek tylko przy realnym błędzie komunikacji/parsowania.
     */
    LoanExtractionResult extract(String emailText);
}
