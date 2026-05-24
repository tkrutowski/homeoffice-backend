package net.focik.homeoffice.devices.domain;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.audit.AuditAction;
import net.focik.homeoffice.audit.AuditLog;
import net.focik.homeoffice.devices.domain.model.Computer;
import net.focik.homeoffice.devices.domain.port.primary.*;
import net.focik.homeoffice.utils.share.ActiveStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@AllArgsConstructor
@Component
public class ComputerFacade implements FindComputerUseCase, SaveComputerUseCase, DeleteComputerUseCase {
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
    @AuditLog(action = AuditAction.CREATE, entityType = "Computer")
    public Computer add(Computer computer) {
        return computerService.add(computer);
    }

    @Override
    @AuditLog(action = AuditAction.UPDATE, entityType = "Computer")
    public Computer update(Computer computer) {
        return computerService.update(computer);
    }

    @Override
    @AuditLog(action = AuditAction.UPDATE, entityType = "Computer")
    public Computer updateStatus(Integer idDevice, ActiveStatus status) {
        return computerService.updateStatus(idDevice, status);
    }

    @Override
    @AuditLog(action = AuditAction.DELETE, entityType = "Computer")
    public void deleteComputer(Integer idComputer) {
        computerService.deleteComputer(idComputer);
    }
}
