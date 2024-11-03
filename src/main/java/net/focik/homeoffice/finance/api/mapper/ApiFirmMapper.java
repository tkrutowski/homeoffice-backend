package net.focik.homeoffice.finance.api.mapper;

import net.focik.homeoffice.finance.api.dto.FirmDto;
import net.focik.homeoffice.finance.domain.firm.Firm;
import org.springframework.stereotype.Component;

public class ApiFirmMapper {
    public static Firm toDomain(FirmDto dto) {
        Firm build = Firm.builder()
                .id(dto.getId())
                .name(dto.getName())
                .phone(dto.getPhone())
                .phone2(dto.getPhone2())
                .mail(dto.getMail())
                .fax(dto.getFax())
                .otherInfo(dto.getOtherInfo())
                .build();
        build.setAddress(dto.getCity(), dto.getStreet(), dto.getZip());
        return build;
    }

    public static FirmDto toDto(Firm c) {
        return FirmDto.builder()
                .id(c.getId())
                .name(c.getName())
                .phone(convertIfNull(c.getPhone()))
                .phone2(convertIfNull(c.getPhone2()))
                .otherInfo(convertIfNull(c.getOtherInfo()))
                .mail(convertIfNull(c.getMail()))
                .fax(convertIfNull(c.getFax()))
                .www(convertIfNull(c.getWww()))
                .city(convertIfNull(c.getAddress().getCity()))
                .street(convertIfNull(c.getAddress().getStreet()))
                .zip(convertIfNull(c.getAddress().getZip()))
                .build();
    }

    private static String convertIfNull(String valueToCheck) {
        return valueToCheck == null ? "" : valueToCheck;
    }
}
