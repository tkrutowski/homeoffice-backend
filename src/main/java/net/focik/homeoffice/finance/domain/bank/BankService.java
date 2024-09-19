package net.focik.homeoffice.finance.domain.bank;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.addresses.api.internal.AddressEndpoint;
import net.focik.homeoffice.addresses.domain.Address;
import net.focik.homeoffice.finance.domain.bank.port.secondary.BankRepository;
import net.focik.homeoffice.finance.domain.exception.BankAlreadyExistException;
import net.focik.homeoffice.finance.domain.exception.BankCanNotBeDeletedException;
import net.focik.homeoffice.finance.domain.exception.BankNotFoundException;
import net.focik.homeoffice.finance.domain.loan.Loan;
import net.focik.homeoffice.finance.domain.loan.LoanFacade;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class BankService {

    private final BankRepository bankRepository;
    private final AddressEndpoint addressEndpoint;
    LoanFacade loanFacade;

    @Transactional
    public Bank addBank(Bank bank) {
        Address address = null;
        validate(bank);
        if (isAddress(bank)) {
            address = addressEndpoint.addAddress(bank.getAddress());
            bank.setAddress(address);
        }

        Bank saved = bankRepository.save(bank);
        if (address != null) {
            saved.setAddress(address);
        }
        return saved;
    }

    private boolean isAddress(Bank bank) {
        return StringUtils.isNotEmpty(bank.getAddress().getCity()) || StringUtils.isNotEmpty(bank.getAddress().getStreet())
                || StringUtils.isNotBlank(bank.getAddress().getZip());
    }

    @Transactional
    public Bank updateBank(Bank bank) {
        Bank byId = findById(bank.getId(), false);
        Address address = null;
        bank.getAddress().setId(byId.getAddress().getId());
        validate(bank);


        if (bank.getAddress().getId() > 0) {
            address = addressEndpoint.updateAddress(bank.getAddress());
            bank.setAddress(address);
        }

        if (bank.getAddress().getId() == 0 && isAddress(bank)) {
            address = addressEndpoint.addAddress(bank.getAddress());
            bank.setAddress(address);
        }

        Bank saved = bankRepository.save(bank);
        if (address != null) {
            saved.setAddress(address);
        }
        return saved;
    }

    private void validate(Bank bank) {
        Optional<Bank> byName = bankRepository.findByName(bank.getName());
        if (byName.isPresent() && byName.get().getId() != bank.getId()) {
            throw new BankAlreadyExistException("Bank o nazwie " + bank.getName() + " już istnieje.");
        }
    }

    @Transactional
    public void deleteBank(Integer id) {
        Bank byId = findById(id, false);
        canBeDeleted(id);
        addressEndpoint.deleteAddress(byId.getAddress().getId());
        bankRepository.delete(id);

    }

    private void canBeDeleted(Integer id) {
        List<Loan> loansByBank = loanFacade.getLoansByStatus(PaymentStatus.ALL, false)
                .stream().filter(loan -> loan.getBank().getId()==id)
                .collect(Collectors.toList());
        if(!loansByBank.isEmpty()){
             throw new BankCanNotBeDeletedException("kredyty");
        }
    }

    public Bank findById(Integer id, Boolean isAddress) {
        Optional<Bank> byId = bankRepository.findById(id);
        if (byId.isEmpty()) {
            throw new BankNotFoundException("id", String.valueOf(id));
        }
        if (isAddress != null && isAddress && byId.get().getAddress().getId() > 0) {
            byId.get().setAddress(addressEndpoint.getAddress(byId.get().getAddress().getId()));
        }
        return byId.get();
    }

    public Bank findByName(String name, Boolean isAddress) {
        Optional<Bank> byName = bankRepository.findByName(name);
        if (byName.isEmpty()) {
            throw new BankNotFoundException("nazwa", String.valueOf(name));
        }
        if (isAddress != null && isAddress) {
            byName.get().setAddress(addressEndpoint.getAddress(byName.get().getAddress().getId()));
        }
        return byName.get();
    }

    public List<Bank> findByAll(Boolean isGetAddress) {
        List<Bank> bankList = bankRepository.findAll();

        if (isGetAddress != null && isGetAddress) {
            bankList.stream()
                    .filter(bank -> bank.getAddress().getId() > 0)
                    .forEach(bank -> bank.setAddress(addressEndpoint.getAddress(bank.getAddress().getId())));
        }

        return bankList;
    }

}
