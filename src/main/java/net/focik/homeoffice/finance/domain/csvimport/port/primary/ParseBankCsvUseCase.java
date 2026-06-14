package net.focik.homeoffice.finance.domain.csvimport.port.primary;

import net.focik.homeoffice.finance.api.dto.BankCsvImportResponse;

public interface ParseBankCsvUseCase {
    String startImportAsync(byte[] fileContent, int idUser);

    BankCsvImportResponse getImportResult(String jobId);
}
