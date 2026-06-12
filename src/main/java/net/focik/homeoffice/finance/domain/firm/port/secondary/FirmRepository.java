package net.focik.homeoffice.finance.domain.firm.port.secondary;

import net.focik.homeoffice.finance.domain.firm.Firm;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public interface FirmRepository {

    Firm save(Firm firm);

    void delete(Integer id);

    List<Firm> findAll();

    Optional<Firm> findById(Integer id);

    Optional<Firm> findByName(String name);

    Optional<Firm> findByNameContaining(String name);

}
