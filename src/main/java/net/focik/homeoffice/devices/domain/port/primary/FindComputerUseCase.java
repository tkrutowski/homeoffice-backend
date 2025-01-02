package net.focik.homeoffice.devices.domain.port.primary;

import net.focik.homeoffice.devices.domain.model.Computer;
import net.focik.homeoffice.utils.share.ActiveStatus;

import java.util.List;

public interface FindComputerUseCase {
    Computer getComputerBuUser(int idUser);

    Computer getComputerById(int id);

    List<Computer> getComputers(ActiveStatus activeStatus);
}
