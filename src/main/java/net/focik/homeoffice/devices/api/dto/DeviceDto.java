package net.focik.homeoffice.devices.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.*;
import net.focik.homeoffice.devices.domain.model.DeviceType;
import net.focik.homeoffice.finance.api.dto.FirmDto;
import net.focik.homeoffice.utils.share.ActiveStatus;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@ToString
public class DeviceDto {
    private Integer id;
    private DeviceType deviceType;
    private FirmDto firm;
    private String name;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate purchaseDate;
    private Number purchaseAmount;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate sellDate;
    private Number sellAmount;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate warrantyEndDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate insuranceEndDate;
    private String otherInfo;
    private ActiveStatus activeStatus;
    @JsonDeserialize(as = LinkedHashMap.class)
    private Map<String, String> details;
    private String imageUrl;
}