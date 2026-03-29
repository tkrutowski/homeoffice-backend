package net.focik.homeoffice.goahead.api.mapper;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.goahead.api.dto.CostDto;
import net.focik.homeoffice.goahead.domain.cost.Cost;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApiCostMapper {

    private final ModelMapper mapper;

    public Cost toDomain(CostDto dto) {
        return mapper.map(dto, Cost.class);
    }

    public CostDto toDto(Cost cost) {
        return mapper.map(cost, CostDto.class);
    }
}
