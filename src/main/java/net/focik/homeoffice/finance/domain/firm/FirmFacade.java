package net.focik.homeoffice.finance.domain.firm;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.audit.AuditAction;
import net.focik.homeoffice.audit.AuditLog;
import net.focik.homeoffice.finance.domain.firm.port.primary.AddFirmUseCase;
import net.focik.homeoffice.finance.domain.firm.port.primary.DeleteFirmUseCase;
import net.focik.homeoffice.finance.domain.firm.port.primary.GetFirmUseCase;
import net.focik.homeoffice.finance.domain.firm.port.primary.UpdateFirmUseCase;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
public class FirmFacade implements AddFirmUseCase, UpdateFirmUseCase, GetFirmUseCase, DeleteFirmUseCase {

    private final FirmService firmService;

    @Override
    @AuditLog(action = AuditAction.CREATE, entityType = "Firm")
    public Firm addFirm(Firm firm) {
        return firmService.addFirm(firm);
    }

    @Override
    @AuditLog(action = AuditAction.UPDATE, entityType = "Firm")
    public Firm updateFirm(Firm firm) {
        return firmService.updateFirm(firm);
    }

    @Override
    @AuditLog(action = AuditAction.DELETE, entityType = "Firm")
    public void deleteFirm(Integer id) {
        firmService.deleteFirm(id);
    }

    @Override
    public Firm findById(Integer id) {
        return firmService.findById(id);
    }

    @Override
    public Firm findByName(String name) {
        return firmService.findByName(name);
    }

    @Override
    public List<Firm> findByAll() {
        return firmService.findByAll();
    }

}
