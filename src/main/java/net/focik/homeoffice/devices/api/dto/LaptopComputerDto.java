package net.focik.homeoffice.devices.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LaptopComputerDto extends ComputerDto {
    private String cpu;
    private String gpu;
    private String ram;
    private String storage;
    private String display;
}
