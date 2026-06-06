package net.focik.homeoffice.finance.infrastructure.mapper;

import net.focik.homeoffice.finance.domain.transaction.model.TransactionLabel;
import net.focik.homeoffice.finance.infrastructure.dto.TransactionLabelDbDto;
import org.springframework.stereotype.Component;

@Component
public class JpaTransactionLabelMapper {

    public TransactionLabel toDomain(TransactionLabelDbDto dto) {
        return TransactionLabel.builder()
                .id(dto.getId())
                .name(dto.getName())
                .build();
    }

    public TransactionLabelDbDto toDto(TransactionLabel domain) {
        return TransactionLabelDbDto.builder()
                .id(domain.getId())
                .name(domain.getName())
                .build();
    }
}
