package net.focik.homeoffice.finance.api.mapper;

import net.focik.homeoffice.finance.api.dto.BankTransactionDto;
import net.focik.homeoffice.finance.domain.exception.BankTransactionNotValidException;
import net.focik.homeoffice.finance.domain.transaction.model.BankTransaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ApiBankTransactionMapper {

    public BankTransaction toDomain(BankTransactionDto dto) {
        valid(dto);
        return BankTransaction.builder()
                .id(dto.getId())
                .idFirm(dto.getIdFirm())
                .idUser(dto.getIdUser())
                .purchaseIds(dto.getPurchaseIds())
                .description(dto.getDescription())
                .transactionDate(dto.getTransactionDate())
                .amount(BigDecimal.valueOf(Double.parseDouble(dto.getAmount())))
                .transactionType(dto.getTransactionType())
                .transactionCategory(dto.getTransactionCategory())
                .transactionLabel(dto.getTransactionLabel())
                .boughtOnCredit(dto.isBoughtOnCredit())
                .build();
    }

    public BankTransactionDto toDto(BankTransaction domain) {
        return BankTransactionDto.builder()
                .id(domain.getId())
                .idFirm(domain.getIdFirm())
                .idUser(domain.getIdUser())
                .purchaseIds(domain.getPurchaseIds())
                .description(domain.getDescription())
                .transactionDate(domain.getTransactionDate())
                .amount(String.format("%.2f", domain.getAmount()).replace(",", "."))
                .transactionType(domain.getTransactionType())
                .transactionCategory(domain.getTransactionCategory() != null ?
                    domain.getTransactionCategory() : null)
                .transactionLabel(domain.getTransactionLabel() != null ?
                    domain.getTransactionLabel() : null)
                .boughtOnCredit(domain.isBoughtOnCredit())
                .build();
    }

    private void valid(BankTransactionDto dto) {
        if (dto.getTransactionDate() == null)
            throw new BankTransactionNotValidException("Transaction date can't be empty.");
        if (dto.getAmount().isEmpty())
            throw new BankTransactionNotValidException("Amount can't be empty.");
    }
}
