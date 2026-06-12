package net.focik.homeoffice.finance.api.mapper;

import net.focik.homeoffice.finance.api.dto.BankTransactionImportResponse;
import net.focik.homeoffice.finance.api.dto.ImportErrorDto;
import net.focik.homeoffice.finance.domain.transaction.model.TransactionImportResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ApiTransactionImportMapper {

    public BankTransactionImportResponse toResponse(TransactionImportResult result) {
        return BankTransactionImportResponse.builder()
                .successCount(result.getSuccessCount())
                .failedCount(result.getFailedCount())
                .totalProcessed(result.getTotalProcessed())
                .status(result.getStatus())
                .errors(mapErrors(result.getErrors()))
                .importedTransactions(result.getImportedTransactions().stream().map(ApiBankTransactionMapper::toDto).collect(Collectors.toList()))
                .importDateTime(result.getImportDateTime())
                .build();
    }

    private List<ImportErrorDto> mapErrors(List<net.focik.homeoffice.finance.domain.transaction.model.TransactionImportError> errors) {
        return errors.stream()
                .map(error -> ImportErrorDto.builder()
                        .rowNumber(error.getRowNumber())
                        .errorMessage(error.getErrorMessage())
                        .csvLineContent(error.getCsvLineContent())
                        .build())
                .collect(Collectors.toList());
    }
}
