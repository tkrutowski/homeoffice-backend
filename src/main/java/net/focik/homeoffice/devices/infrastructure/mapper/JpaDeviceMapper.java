package net.focik.homeoffice.devices.infrastructure.mapper;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.devices.domain.model.Device;
import net.focik.homeoffice.devices.domain.model.DeviceType;
import net.focik.homeoffice.devices.infrastructure.dto.DeviceDbDto;
import net.focik.homeoffice.devices.infrastructure.dto.DeviceTypeDbDto;
import net.focik.homeoffice.finance.domain.firm.Firm;
import net.focik.homeoffice.finance.infrastructure.dto.FirmDbDto;
import net.focik.homeoffice.utils.StringHelper;
import org.javamoney.moneta.Money;
import org.javamoney.moneta.spi.MoneyUtils;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaDeviceMapper {
    private final ModelMapper mapper;

    public DeviceDbDto toDto(Device device) {
        return DeviceDbDto.builder()
                .id(device.getId())
                .deviceType(mapper.map(device.getDeviceType(), DeviceTypeDbDto.class))
                .firm(mapper.map(device.getFirm(), FirmDbDto.class))
                .name(device.getName())
                .purchaseDate(device.getPurchaseDate())
                .purchaseAmount(MoneyUtils.getBigDecimal(device.getPurchaseAmount().getNumber()))
                .sellDate(device.getSellDate())
                .sellAmount(MoneyUtils.getBigDecimal(device.getSellAmount().getNumber()))
                .warrantyEndDate(device.getWarrantyEndDate())
                .insuranceEndDate(device.getInsuranceEndDate())
                .otherInfo(device.getOtherInfo())
                .activeStatus(device.getActiveStatus())
                .details(StringHelper.mapToString(device.getDetails(), ";;"))
                .imageUrl(device.getImageUrl())
                .build();
    }

    public Device toDomain(DeviceDbDto dto) {
        return Device.builder()
                .id(dto.getId())
                .firm(mapper.map(dto.getFirm(), Firm.class))
                .deviceType(mapper.map(dto.getDeviceType(), DeviceType.class))
                .name(dto.getName())
                .purchaseDate(dto.getPurchaseDate())
                .purchaseAmount(Money.of(dto.getPurchaseAmount(), "PLN"))
                .sellDate(dto.getSellDate())
                .sellAmount(Money.of(dto.getSellAmount(), "PLN"))
                .warrantyEndDate(dto.getWarrantyEndDate())
                .insuranceEndDate(dto.getInsuranceEndDate())
                .otherInfo(dto.getOtherInfo())
                .activeStatus(dto.getActiveStatus())
                .imageUrl(dto.getImageUrl())
                .details(StringHelper.stringToMap(dto.getDetails(), ";;"))
                .build();
    }

}