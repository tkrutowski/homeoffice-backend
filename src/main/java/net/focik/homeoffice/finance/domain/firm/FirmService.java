package net.focik.homeoffice.finance.domain.firm;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.addresses.api.internal.AddressEndpoint;
import net.focik.homeoffice.addresses.domain.Address;
import net.focik.homeoffice.finance.domain.exception.BankAlreadyExistException;
import net.focik.homeoffice.finance.domain.exception.BankNotFoundException;
import net.focik.homeoffice.finance.domain.exception.FirmCanNotBeDeletedException;
import net.focik.homeoffice.finance.domain.exception.FirmNotFoundException;
import net.focik.homeoffice.finance.domain.firm.port.secondary.FirmRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
class FirmService {

    private final FirmRepository firmRepository;
    private final AddressEndpoint addressEndpoint;

    @Transactional
    public Firm addFirm(Firm firm) {
        Address address = null;
        validate(firm);
        if (isAddress(firm)) {
            address = addressEndpoint.addAddress(firm.getAddress());
            firm.setAddress(address);
        }

        Firm saved = firmRepository.save(firm);
        if (address != null) {
            saved.setAddress(address);
        }
        return saved;
    }

    private boolean isAddress(Firm firm) {
        return StringUtils.isNotEmpty(firm.getAddress().getCity()) || StringUtils.isNotEmpty(firm.getAddress().getStreet())
                || StringUtils.isNotBlank(firm.getAddress().getZip());
    }

    @Transactional
    public Firm updateFirm(Firm firm) {
        Firm byId = findById(firm.getId(), false);
        Address address = null;
        firm.getAddress().setId(byId.getAddress().getId());
        validate(firm);


        if (firm.getAddress().getId() > 0) {
            address = addressEndpoint.updateAddress(firm.getAddress());
            firm.setAddress(address);
        }

        if (firm.getAddress().getId() == 0 && isAddress(firm)) {
            address = addressEndpoint.addAddress(firm.getAddress());
            firm.setAddress(address);
        }

        Firm saved = firmRepository.save(firm);
        if (address != null) {
            saved.setAddress(address);
        }
        return saved;
    }

    private void validate(Firm firm) {
        Optional<Firm> byName = firmRepository.findByName(firm.getName());
        if (byName.isPresent() && byName.get().getId() != firm.getId()) {
            throw new BankAlreadyExistException("Firma o nazwie " + firm.getName() + " już istnieje.");
        }
    }

    @Transactional
    public void deleteFirm(Integer id) {
        Firm byId = findById(id, false);
        if (canBeDeleted(id)) {
            addressEndpoint.deleteAddress(byId.getAddress().getId());
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

    public Firm findById(Integer id, Boolean isAddress) {
        Optional<Firm> byId = firmRepository.findById(id);
        if (byId.isEmpty()) {
            throw new FirmNotFoundException("id", String.valueOf(id));
        }
        if (isAddress != null && isAddress && byId.get().getAddress().getId() > 0) {
            byId.get().setAddress(addressEndpoint.getAddress(byId.get().getAddress().getId()));
        }
        return byId.get();
    }

    public Firm findByName(String name, Boolean isAddress) {
        Optional<Firm> byName = firmRepository.findByName(name);
        if (byName.isEmpty()) {
            throw new BankNotFoundException("nazwa", String.valueOf(name));
        }
        if (isAddress != null && isAddress) {
            byName.get().setAddress(addressEndpoint.getAddress(byName.get().getAddress().getId()));
        }
        return byName.get();
    }

    public List<Firm> findByAll(Boolean isGetAddress) {
        List<Firm> bankList = firmRepository.findAll();

        if (isGetAddress != null && isGetAddress) {
            bankList.stream()
                    .filter(bank -> bank.getAddress().getId() > 0)
                    .forEach(bank -> bank.setAddress(addressEndpoint.getAddress(bank.getAddress().getId())));
        }

        return bankList;
    }

}
