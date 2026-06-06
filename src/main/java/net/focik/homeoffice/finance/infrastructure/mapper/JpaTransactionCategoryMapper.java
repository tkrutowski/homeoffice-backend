package net.focik.homeoffice.finance.infrastructure.mapper;

import net.focik.homeoffice.finance.domain.transaction.model.TransactionCategory;
import net.focik.homeoffice.finance.infrastructure.dto.TransactionCategoryDbDto;
import org.springframework.stereotype.Component;

@Component
public class JpaTransactionCategoryMapper {

    public TransactionCategory toDomain(TransactionCategoryDbDto dto) {
        return TransactionCategory.builder()
                .id(dto.getId())
                .name(dto.getName())
                .type(dto.getType())
                .color(dto.getColor())
                .icon(dto.getIcon())
                .build();
    }

    public TransactionCategoryDbDto toDto(TransactionCategory domain) {
        return TransactionCategoryDbDto.builder()
                .id(domain.getId())
                .name(domain.getName())
                .type(domain.getType())
                .color(domain.getColor())
                .icon(domain.getIcon())
                .build();
    }
}
