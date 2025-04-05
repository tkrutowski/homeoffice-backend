package net.focik.homeoffice.finance.domain.bank;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.finance.domain.bank.port.primary.AddBankUseCase;
import net.focik.homeoffice.finance.domain.bank.port.primary.DeleteBankUseCase;
import net.focik.homeoffice.finance.domain.bank.port.primary.GetBankUseCase;
import net.focik.homeoffice.finance.domain.bank.port.primary.UpdateBankUseCase;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
public class BankFacade implements AddBankUseCase, UpdateBankUseCase, GetBankUseCase, DeleteBankUseCase {

    private final BankService bankService;

    @Override
    public Bank addBank(Bank bank) {
        return bankService.addBank(bank);
    }

    @Override
    public Bank updateBank(Bank bank) {
        return bankService.updateBank(bank);
    }

    @Override
    public void deleteBank(Integer id) {
        bankService.deleteBank(id);
    }

    @Override
    public Bank findById(Integer id) {
        return bankService.findById(id);
    }

    @Override
    public Bank findByName(String name) {
        return bankService.findByName(name);
    }

    @Override
    public List<Bank> findByAll() {
        return bankService.findByAll();
    }

}
