package net.focik.homeoffice.finance.api.mapper;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.addresses.domain.Address;
import net.focik.homeoffice.finance.api.dto.AddressDto;
import net.focik.homeoffice.finance.api.dto.FirmDto;
import net.focik.homeoffice.finance.domain.firm.Firm;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApiFirmMapper {
    private final ModelMapper mapper;

    public Firm toDomain(FirmDto dto) {
        return Firm.builder()
                .id(dto.getId())
                .name(dto.getName())
                .phone(dto.getPhone())
                .phone2(dto.getPhone2())
                .mail(dto.getMail())
                .fax(dto.getFax())
                .otherInfo(dto.getOtherInfo())
                .address(dto.getAddress() != null ? mapper.map(dto.getAddress(), Address.class) : null)
                .build();
    }

    public FirmDto toDto(Firm firm) {
        return FirmDto.builder()
                .id(firm.getId())
                .name(firm.getName())
                .phone(convertIfNull(firm.getPhone()))
                .phone2(convertIfNull(firm.getPhone2()))
                .otherInfo(convertIfNull(firm.getOtherInfo()))
                .mail(convertIfNull(firm.getMail()))
                .fax(convertIfNull(firm.getFax()))
                .www(convertIfNull(firm.getWww()))
                .address(firm.getAddress() != null ? mapper.map(firm.getAddress(), AddressDto.class) : null)
                .build();
    }

    private static String convertIfNull(String valueToCheck) {
        return valueToCheck == null ? "" : valueToCheck;
    }
}
