package net.focik.homeoffice.finance.domain.bank.port.primary;

import net.focik.homeoffice.finance.domain.bank.Bank;

import java.util.List;

public interface GetBankUseCase {
    Bank findById(Integer id);

    Bank findByName(String name);

    List<Bank> findByAll();
}
