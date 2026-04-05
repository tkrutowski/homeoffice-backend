package net.focik.homeoffice.goahead.domain.invoice.ksef.model;

import java.time.LocalDate;

public record FindKsefInvoiceRequest(LocalDate fromDate, LocalDate toDate) {
}
