package net.focik.homeoffice.devices.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class LaptopComputer extends Computer {
    private String cpu;
    private String gpu;
    private String ram;
    private String storage;
    private String display;
}
