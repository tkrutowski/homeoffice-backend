package net.focik.homeoffice.devices.domain.port.primary;

import net.focik.homeoffice.devices.domain.model.Computer;
import net.focik.homeoffice.utils.share.ActiveStatus;

public interface SaveComputerUseCase {
    Computer add(Computer computer);

    Computer update(Computer computer);

    Computer updateStatus(Integer id, ActiveStatus status);
}
