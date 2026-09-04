package net.focik.homeoffice.devices.api.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.focik.homeoffice.utils.share.ActiveStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "computerType", include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
    @JsonSubTypes.Type(value = DesktopComputerDto.class, name = "DESKTOP"),
    @JsonSubTypes.Type(value = LaptopComputerDto.class, name = "LAPTOP")
})
public abstract class ComputerDto {
    protected Integer id;
    protected Integer idUser;
    protected String name;
    protected ActiveStatus activeStatus;
    protected String info;
}