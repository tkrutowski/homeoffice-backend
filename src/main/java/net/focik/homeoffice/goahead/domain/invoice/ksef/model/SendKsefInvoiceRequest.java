package net.focik.homeoffice.goahead.domain.invoice.ksef.model;

import java.util.List;

public record SendKsefInvoiceRequest(List<Integer> invoicesIds) {
}
