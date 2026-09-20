package net.focik.homeoffice.goahead.domain.invoice;

import net.focik.homeoffice.async.AsyncTaskError;

import java.util.List;

/**
 * @param imported      faktury zapisane w systemie
 * @param found         wszystkie faktury zwrócone przez KSeF
 * @param duplicates    faktury już obecne w systemie (po numerze KSeF)
 * @param skipped       faktury celowo pominięte (korygujące/inne niż zwykła VAT, waluta obca)
 * @param errors        faktury, których nie udało się zaimportować
 */
public record KsefInvoiceImportResult(List<Invoice> imported, int found, int duplicates, int skipped,
                                      List<AsyncTaskError> errors) {}
