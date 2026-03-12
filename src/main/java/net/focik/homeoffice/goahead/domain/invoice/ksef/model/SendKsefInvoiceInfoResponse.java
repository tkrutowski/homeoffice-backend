package net.focik.homeoffice.goahead.domain.invoice.ksef.model;

import net.focik.homeoffice.goahead.domain.invoice.Invoice;

import java.util.List;

public record SendKsefInvoiceInfoResponse(List<Invoice> invoices, int invoiceCount, int successInvoiceCount, int failedInvoiceCount) {
}
