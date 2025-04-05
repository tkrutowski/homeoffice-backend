package net.focik.homeoffice.finance.domain.bank;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.finance.domain.bank.port.secondary.BankRepository;
import net.focik.homeoffice.finance.domain.card.Card;
import net.focik.homeoffice.finance.domain.card.CardFacade;
import net.focik.homeoffice.finance.domain.exception.BankAlreadyExistException;
import net.focik.homeoffice.finance.domain.exception.BankCanNotBeDeletedException;
import net.focik.homeoffice.finance.domain.exception.BankNotFoundException;
import net.focik.homeoffice.finance.domain.loan.Loan;
import net.focik.homeoffice.finance.domain.loan.LoanFacade;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
class BankService {

    private final BankRepository bankRepository;
    private final LoanFacade loanFacade;
    private final CardFacade cardFacade;

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
        if(canBeDeleted(id)){
            bankRepository.delete(id);
        }
    }

    private boolean canBeDeleted(Integer idBank) {
        log.debug("Checking if bank with ID {} can be deleted...", idBank);
        List<Loan> loansByBank = loanFacade.getLoansByBank(idBank);
        if(!loansByBank.isEmpty()){
            log.warn("Bank with ID {} cannot be deleted — associated loans found (count: {}).", idBank, loansByBank.size());
             throw new BankCanNotBeDeletedException("kredyty.");
        }
        List<Card> cardsByBank = cardFacade.getCardsByBank(idBank);
        if(!cardsByBank.isEmpty()){
            log.warn("Bank with ID {} cannot be deleted — associated cards found (count: {}).", idBank, cardsByBank.size());
            throw new BankCanNotBeDeletedException("karty.");
        }
        return true;
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
