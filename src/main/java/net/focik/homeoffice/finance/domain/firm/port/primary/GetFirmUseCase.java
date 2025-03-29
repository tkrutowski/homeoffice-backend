package net.focik.homeoffice.finance.domain.firm.port.primary;

import net.focik.homeoffice.finance.domain.firm.Firm;

import java.util.List;

public interface GetFirmUseCase {
    Firm findById(Integer id);

    Firm findByName(String name);

    List<Firm> findByAll();
}
