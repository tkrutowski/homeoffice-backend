package net.focik.homeoffice.finance.domain.loanproposal.port.secondary;

import java.util.Optional;

public interface LoanEmailArchivePort {
    /**
     * Zapisuje surowy .eml do trwałego magazynu (S3), do audytu. Zwraca klucz obiektu,
     * albo pusty Optional, gdy rawEml nie zostało dostarczone przez n8n.
     */
    Optional<String> store(String messageId, byte[] rawEml);
}
