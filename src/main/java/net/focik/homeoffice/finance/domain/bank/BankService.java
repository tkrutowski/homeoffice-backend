package net.focik.homeoffice.finance.domain.bank;

import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
class BankService {

    private final BankRepository bankRepository;
    LoanFacade loanFacade;

    @Transactional
    public Bank addBank(Bank bank) {
        validate(bank);
        return bankRepository.save(bank);
    }

    private boolean isAddress(Bank bank) {
        return StringUtils.isNotEmpty(bank.getAddress().getCity()) || StringUtils.isNotEmpty(bank.getAddress().getStreet())
                || StringUtils.isNotBlank(bank.getAddress().getZip());
    }

    @Transactional
    public Bank updateBank(Bank bank) {
        Bank byId = findById(bank.getId());
        bank.getAddress().setId(byId.getAddress().getId());
        validate(bank);
        return bankRepository.save(bank);
    }

    private void validate(Bank bank) {
        Optional<Bank> byName = bankRepository.findByName(bank.getName());
        if (byName.isPresent() && byName.get().getId() != bank.getId()) {
            throw new BankAlreadyExistException("Bank o nazwie " + bank.getName() + " już istnieje.");
        }
    }

    @Transactional
    public void deleteBank(Integer id) {
        canBeDeleted(id);
        bankRepository.delete(id);

    }

    private void canBeDeleted(Integer id) {
        List<Loan> loansByBank = loanFacade.getLoansByStatus(PaymentStatus.ALL, false)
                .stream().filter(loan -> loan.getBank().getId()==id)
                .toList();
        if(!loansByBank.isEmpty()){
             throw new BankCanNotBeDeletedException("kredyty");
        }
    }

    public Bank findById(Integer id) {
        Optional<Bank> byId = bankRepository.findById(id);
        if (byId.isEmpty()) {
            throw new BankNotFoundException("id", String.valueOf(id));
        }
        return byId.get();
    }

    public Bank findByName(String name) {
        Optional<Bank> byName = bankRepository.findByName(name);
        if (byName.isEmpty()) {
            throw new BankNotFoundException("nazwa", String.valueOf(name));
        }
        return byName.get();
    }

    public List<Bank> findByAll() {
        return bankRepository.findAll();
    }

}
