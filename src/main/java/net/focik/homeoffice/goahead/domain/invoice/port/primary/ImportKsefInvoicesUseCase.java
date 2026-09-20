package net.focik.homeoffice.goahead.domain.invoice.port.primary;

import net.focik.homeoffice.goahead.domain.invoice.KsefInvoiceImportResult;

import java.time.LocalDate;

public interface ImportKsefInvoicesUseCase {
    /**
     * Pobiera z KSeF faktury sprzedażowe (także wystawione poza aplikacją) i zapisuje te, których jeszcze nie ma.
     */
    KsefInvoiceImportResult importKsefInvoices(LocalDate fromDate, LocalDate toDate);
}
