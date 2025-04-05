package net.focik.homeoffice.finance.domain.firm;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.devices.domain.DeviceFacade;
import net.focik.homeoffice.devices.domain.model.Device;
import net.focik.homeoffice.finance.domain.exception.BankNotFoundException;
import net.focik.homeoffice.finance.domain.exception.FirmAlreadyExistException;
import net.focik.homeoffice.finance.domain.exception.FirmCanNotBeDeletedException;
import net.focik.homeoffice.finance.domain.exception.FirmNotFoundException;
import net.focik.homeoffice.finance.domain.fee.Fee;
import net.focik.homeoffice.finance.domain.fee.FeeFacade;
import net.focik.homeoffice.finance.domain.firm.port.secondary.FirmRepository;
import net.focik.homeoffice.finance.domain.purchase.Purchase;
import net.focik.homeoffice.finance.domain.purchase.PurchaseFacade;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
class FirmService {
    private final FeeFacade feeFacade;
    private final DeviceFacade deviceFacade;
    private final PurchaseFacade purchaseFacade;
    private final FirmRepository firmRepository;

    @Transactional
    public Firm addFirm(Firm firm) {
        validate(firm);
        return firmRepository.save(firm);
    }

    @Transactional
    public Firm updateFirm(Firm firm) {
        return firmRepository.save(firm);
    }

    private void validate(Firm firm) {
        Optional<Firm> byName = firmRepository.findByName(firm.getName());
        if (byName.isPresent() && byName.get().getId() != firm.getId()) {
            throw new FirmAlreadyExistException("Firma o nazwie " + firm.getName() + " już istnieje.");
        }
    }

    @Transactional
    public void deleteFirm(Integer idFirm) {
        log.debug("Deleting firm {}", idFirm);
        if (canBeDeleted(idFirm)) {
            firmRepository.delete(idFirm);
        }
    }

    private boolean canBeDeleted(Integer id) {
        log.debug("Checking if firm with ID {} can be deleted...", id);
        List<Fee> feesByFirm = feeFacade.getFeesByFirm(id, false);
        if (!feesByFirm.isEmpty()) {
            log.warn("Firm with ID {} cannot be deleted — associated fees found (count: {}).", id, feesByFirm.size());
            throw new FirmCanNotBeDeletedException("opłaty");
        }
        List<Device> devicesByFirm = deviceFacade.getDevicesByFirm(id);
        if (!devicesByFirm.isEmpty()) {
            log.warn("Firm with ID {} cannot be deleted — associated devices found (count: {}).", id, devicesByFirm.size());
            throw new FirmCanNotBeDeletedException("urządzenia");
        }
        List<Purchase> purchasesByFirm = purchaseFacade.getPurchasesByFirm(id);
        if (!purchasesByFirm.isEmpty()) {
            log.warn("Firm with ID {} cannot be deleted — associated purchases found (count: {}).", id, purchasesByFirm.size());
            throw new FirmCanNotBeDeletedException("zakupy");
        }
        log.debug("Firm with ID {} can be safely deleted.", id);
        return true;
    }

    public Firm findById(Integer id) {
        Optional<Firm> byId = firmRepository.findById(id);
        if (byId.isEmpty()) {
            throw new FirmNotFoundException("id", String.valueOf(id));
        }
        return byId.get();
    }

    public Firm findByName(String name) {
        Optional<Firm> byName = firmRepository.findByName(name);
        if (byName.isEmpty()) {
            throw new BankNotFoundException("nazwa", String.valueOf(name));
        }
        return byName.get();
    }

    public List<Firm> findByAll() {
        return firmRepository.findAll();
    }

}
