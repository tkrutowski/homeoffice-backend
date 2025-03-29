package net.focik.homeoffice.finance.domain.firm;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.addresses.api.internal.AddressEndpoint;
import net.focik.homeoffice.finance.domain.exception.*;
import net.focik.homeoffice.finance.domain.firm.port.secondary.FirmRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
class FirmService {

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
    public void deleteFirm(Integer id) {
        if (canBeDeleted(id)) {
            firmRepository.delete(id);
        }
    }

    private boolean canBeDeleted(Integer id) {
        //TODO check opłaty
        //TODO sprawdzić czy nie ma ZAKUPÓW
        if (id < 0) {
            throw new FirmCanNotBeDeletedException("opłaty");
        }
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
