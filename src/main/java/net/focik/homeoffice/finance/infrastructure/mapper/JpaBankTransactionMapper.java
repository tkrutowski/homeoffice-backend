package net.focik.homeoffice.finance.infrastructure.mapper;

import net.focik.homeoffice.finance.domain.transaction.model.BankTransaction;
import net.focik.homeoffice.finance.domain.transaction.model.TransactionCategory;
import net.focik.homeoffice.finance.domain.transaction.model.TransactionLabel;
import net.focik.homeoffice.finance.infrastructure.dto.BankTransactionDbDto;
import org.springframework.stereotype.Component;

@Component
public class JpaBankTransactionMapper {

    public BankTransaction toDomain(BankTransactionDbDto dto) {
        return BankTransaction.builder()
                .id(dto.getId())
                .idFirm(dto.getIdFirm())
                .idUser(dto.getIdUser())
                .purchaseId(dto.getPurchaseId())
                .description(dto.getDescription())
                .transactionDate(dto.getTransactionDate())
                .amount(dto.getAmount())
                .transactionType(dto.getTransactionType())
                .transactionCategory(dto.getTransactionCategoryId() != null ?
                    TransactionCategory.builder().id(dto.getTransactionCategoryId()).build() : null)
                .transactionLabel(dto.getTransactionLabelIds() != null ?
                    dto.getTransactionLabelIds().stream()
                        .map(id -> TransactionLabel.builder().id(id).build())
                        .toList() : null)
                .boughtOnCredit(dto.isBoughtOnCredit())
                .build();
    }

    public BankTransactionDbDto toDto(BankTransaction domain) {
        return BankTransactionDbDto.builder()
                .id(domain.getId())
                .idFirm(domain.getIdFirm())
                .idUser(domain.getIdUser())
                .purchaseId(domain.getPurchaseId())
                .description(domain.getDescription())
                .transactionDate(domain.getTransactionDate())
                .amount(domain.getAmount())
                .transactionType(domain.getTransactionType())
                .transactionCategoryId(domain.getTransactionCategory() != null ?
                    domain.getTransactionCategory().getId() : null)
                .transactionLabelIds(domain.getTransactionLabel() != null ?
                    domain.getTransactionLabel().stream()
                        .map(TransactionLabel::getId)
                        .toList() : null)
                .boughtOnCredit(domain.isBoughtOnCredit())
                .build();
    }
}
