package net.focik.homeoffice.devices.api.mapper;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.devices.api.dto.DeviceDto;
import net.focik.homeoffice.devices.domain.model.Device;
import net.focik.homeoffice.finance.api.mapper.ApiFirmMapper;
import org.javamoney.moneta.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static net.focik.homeoffice.utils.share.Commons.MIN_LOCAL_DATE;

@Component
@RequiredArgsConstructor
public class ApiDeviceMapper {

    public Device toDomain(DeviceDto dto) {
        return Device.builder()
                .id(dto.getId())
                .deviceType(dto.getDeviceType())
                .firm(ApiFirmMapper.toDomain(dto.getFirm()))
                .name(dto.getName())
                .purchaseDate(dto.getPurchaseDate())
                .purchaseAmount(Money.of(BigDecimal.valueOf(dto.getPurchaseAmount().doubleValue()), "PLN"))
                .sellDate(dto.getSellDate())
                .sellAmount(Money.of(BigDecimal.valueOf(dto.getSellAmount().doubleValue()), "PLN"))
                .warrantyEndDate(dto.getWarrantyEndDate())
                .insuranceEndDate(dto.getInsuranceEndDate())
                .otherInfo(dto.getOtherInfo())
                .activeStatus(dto.getActiveStatus())
                .build();
    }

    public DeviceDto toDto(Device dev) {
        return DeviceDto.builder()
                .id(dev.getId())
                .deviceType(dev.getDeviceType())
                .firm(ApiFirmMapper.toDto(dev.getFirm()))
                .name(dev.getName())
                .purchaseDate(dev.getPurchaseDate())
                .purchaseAmount(dev.getPurchaseAmount().getNumber().doubleValue())
                .sellDate(dev.getSellDate() != null ? dev.getSellDate() : MIN_LOCAL_DATE)
                .sellAmount(dev.getSellAmount().getNumber().doubleValue())
                .warrantyEndDate(dev.getWarrantyEndDate() != null ? dev.getWarrantyEndDate() : MIN_LOCAL_DATE)
                .insuranceEndDate(dev.getInsuranceEndDate() != null ? dev.getInsuranceEndDate() : MIN_LOCAL_DATE)
                .otherInfo(dev.getOtherInfo())
                .activeStatus(dev.getActiveStatus())
                .build();
    }
}