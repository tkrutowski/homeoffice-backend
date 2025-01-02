package net.focik.homeoffice.devices.domain.port.secondary;

import net.focik.homeoffice.devices.domain.model.Computer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public interface ComputerRepository {
    Computer saveComputer(Computer computer);

    Optional<Computer> findComputerById(int id);

    List<Computer> findAllComputers();

    List<Computer> findComputersByUser(int userId);

    void deleteComputer(Integer id);
}
