package net.focik.homeoffice.finance.infrastructure.mapper;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.finance.domain.firm.Firm;
import net.focik.homeoffice.finance.infrastructure.dto.FirmDbDto;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaFirmMapper {
    private final ModelMapper modelMapper;

    public FirmDbDto toDto(Firm firm) {
        return modelMapper.map(firm, FirmDbDto.class);
    }

    public Firm toDomain(FirmDbDto dto) {
        return modelMapper.map(dto, Firm.class);
    }
}