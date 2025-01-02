package net.focik.homeoffice.devices.domain;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.devices.domain.model.Computer;
import net.focik.homeoffice.devices.domain.port.primary.*;
import net.focik.homeoffice.utils.share.ActiveStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@AllArgsConstructor
@Component
public class ComputerFacade implements FindComputerUseCase, SaveComputerUseCase {
    private final ComputerService computerService;

    @Override
    public Computer getComputerBuUser(int idUser) {
        return null;
    }

    @Override
    public Computer getComputerById(int id) {
        return computerService.getComputerById(id);
    }

    @Override
    public List<Computer> getComputers(ActiveStatus status) {
        return computerService.getComputers(status);
    }

    @Override
    public Computer add(Computer computer) {
        return computerService.add(computer);
    }

    @Override
    public Computer update(Computer computer) {
        return computerService.update(computer);
    }

    @Override
    public Computer updateStatus(Integer idDevice, ActiveStatus status) {
        return computerService.updateStatus(idDevice, status);
    }
}
