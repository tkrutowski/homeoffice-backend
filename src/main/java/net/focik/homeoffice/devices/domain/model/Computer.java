package net.focik.homeoffice.devices.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.focik.homeoffice.utils.share.ActiveStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public sealed abstract class Computer permits DesktopComputer, LaptopComputer {
    protected Integer id;
    protected Integer idUser;
    protected String name;
    protected ActiveStatus status;
    protected String info;
}
