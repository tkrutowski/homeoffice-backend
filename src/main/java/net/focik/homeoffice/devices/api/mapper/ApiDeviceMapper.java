package net.focik.homeoffice.devices.api.mapper;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.devices.api.dto.DeviceDto;
import net.focik.homeoffice.devices.domain.model.Device;
import net.focik.homeoffice.finance.api.mapper.ApiFirmMapper;
import org.javamoney.moneta.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ApiDeviceMapper {
    private final ApiFirmMapper firmMapper;

    public Device toDomain(DeviceDto dto) {
        return dto == null ? null : Device.builder()
                .id(dto.getId())
                .deviceType(dto.getDeviceType() != null ? dto.getDeviceType() : null)
                .firm(dto.getFirm() != null ? firmMapper.toDomain(dto.getFirm()) : null)
                .name(dto.getName() != null ? dto.getName() : null)
                .purchaseDate(dto.getPurchaseDate() != null ? dto.getPurchaseDate() : null)
                .purchaseAmount(dto.getPurchaseAmount() != null ? Money.of(BigDecimal.valueOf(dto.getPurchaseAmount().doubleValue()), "PLN") : null)
                .sellDate(dto.getSellDate() != null ? dto.getSellDate() : null)
                .sellAmount(dto.getSellAmount() != null ? Money.of(BigDecimal.valueOf(dto.getSellAmount().doubleValue()), "PLN") : null)
                .warrantyEndDate(dto.getWarrantyEndDate() != null ? dto.getWarrantyEndDate() : null)
                .insuranceEndDate(dto.getInsuranceEndDate() != null ? dto.getInsuranceEndDate() : null)
                .otherInfo(dto.getOtherInfo() != null ? dto.getOtherInfo() : null)
                .activeStatus(dto.getActiveStatus() != null ? dto.getActiveStatus() : null)
                .details(dto.getDetails() != null ? dto.getDetails() : null)
                .imageUrl(dto.getImageUrl() != null ? dto.getImageUrl() : null)
                .files(dto.getFiles() != null ? dto.getFiles() : null)
                .build();
    }

    public List<Device> toDomain(List<DeviceDto> dtos) {
        return dtos.stream()
                .map(this::toDomain)
                .toList();
    }

    public DeviceDto toDto(Device dev) {
        return dev == null ? null : DeviceDto.builder()
                .id(dev.getId())
                .deviceType(dev.getDeviceType() != null ? dev.getDeviceType() : null)
                .firm(dev.getFirm() != null ? firmMapper.toDto(dev.getFirm()) : null)
                .name(dev.getName() != null ? dev.getName() : null)
                .purchaseDate(dev.getPurchaseDate() != null ? dev.getPurchaseDate() : null)
                .purchaseAmount(dev.getPurchaseAmount() != null ? dev.getPurchaseAmount().getNumber().doubleValue() : null)
                .sellDate(dev.getSellDate() != null ? dev.getSellDate() : null)
                .sellAmount(dev.getSellAmount() != null ? dev.getSellAmount().getNumber().doubleValue() : null)
                .warrantyEndDate(dev.getWarrantyEndDate() != null ? dev.getWarrantyEndDate() : null)
                .insuranceEndDate(dev.getInsuranceEndDate() != null ? dev.getInsuranceEndDate() : null)
                .otherInfo(dev.getOtherInfo() != null ? dev.getOtherInfo() : null)
                .activeStatus(dev.getActiveStatus() != null ? dev.getActiveStatus() : null)
                .details(dev.getDetails() != null ? dev.getDetails() : null)
                .imageUrl(dev.getImageUrl() != null ? dev.getImageUrl() : null)
                .files(dev.getFiles() != null ? dev.getFiles() : null)
                .build();
    }

    public List<DeviceDto> toDto(List<Device> devices) {
        return devices.stream()
                .map(this::toDto)
                .toList();
    }
}