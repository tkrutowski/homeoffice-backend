package net.focik.homeoffice.finance.api.mapper;

import net.focik.homeoffice.finance.api.dto.TransactionLabelDto;
import net.focik.homeoffice.finance.domain.transaction.model.TransactionLabel;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ApiTransactionLabelMapper {

    public TransactionLabel toDomain(TransactionLabelDto dto) {
        return TransactionLabel.builder()
                .id(dto.getId())
                .name(dto.getName())
                .build();
    }

    public TransactionLabelDto toDto(TransactionLabel domain) {
        return TransactionLabelDto.builder()
                .id(domain.getId())
                .name(domain.getName())
                .build();
    }

    public List<TransactionLabelDto> toDtoList(List<TransactionLabel> domains) {
        return domains.stream()
                .map(this::toDto)
                .toList();
    }
}
