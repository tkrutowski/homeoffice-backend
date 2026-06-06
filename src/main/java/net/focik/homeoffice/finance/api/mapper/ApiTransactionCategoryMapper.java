package net.focik.homeoffice.finance.api.mapper;

import net.focik.homeoffice.finance.api.dto.TransactionCategoryDto;
import net.focik.homeoffice.finance.domain.transaction.model.TransactionCategory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ApiTransactionCategoryMapper {

    public TransactionCategory toDomain(TransactionCategoryDto dto) {
        return TransactionCategory.builder()
                .id(dto.getId())
                .name(dto.getName())
                .type(dto.getType())
                .color(dto.getColor())
                .icon(dto.getIcon())
                .build();
    }

    public TransactionCategoryDto toDto(TransactionCategory domain) {
        return TransactionCategoryDto.builder()
                .id(domain.getId())
                .name(domain.getName())
                .type(domain.getType())
                .color(domain.getColor())
                .icon(domain.getIcon())
                .build();
    }

    public List<TransactionCategoryDto> toDtoList(List<TransactionCategory> domains) {
        return domains.stream()
                .map(this::toDto)
                .toList();
    }
}
