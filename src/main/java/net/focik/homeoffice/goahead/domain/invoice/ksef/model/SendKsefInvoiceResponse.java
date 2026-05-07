package net.focik.homeoffice.goahead.domain.invoice.ksef.model;

public record SendKsefInvoiceResponse(String ksefNumber, String invoiceNumber, String invoiceHash, String upoXml, int invoiceCount, int successInvoiceCount, int failedInvoiceCount) {
}
