package net.focik.homeoffice.devices.domain.port.primary;

import net.focik.homeoffice.devices.domain.model.Computer;
import net.focik.homeoffice.devices.domain.model.ComputerType;
import net.focik.homeoffice.utils.share.ActiveStatus;

public interface SaveComputerUseCase {
    <T extends Computer> T add(T computer);

    <T extends Computer> T update(T computer);

    Computer updateStatus(Integer id, ActiveStatus status, ComputerType type);
}
