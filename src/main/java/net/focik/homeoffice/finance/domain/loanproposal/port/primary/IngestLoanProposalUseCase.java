package net.focik.homeoffice.finance.domain.loanproposal.port.primary;

import net.focik.homeoffice.finance.domain.loanproposal.LoanProposal;
import net.focik.homeoffice.finance.domain.loanproposal.RawLoanEmail;

public interface IngestLoanProposalUseCase {
    /**
     * Zapisuje nową propozycję (status NEW) i asynchronicznie startuje ekstrakcję danych.
     * Idempotentne po {@link RawLoanEmail#getMessageId()} - powtórne wywołanie dla tej samej
     * wiadomości zwraca istniejący rekord zamiast tworzyć duplikat.
     */
    LoanProposal ingest(RawLoanEmail email);
}
