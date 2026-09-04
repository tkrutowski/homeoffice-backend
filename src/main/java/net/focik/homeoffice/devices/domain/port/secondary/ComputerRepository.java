package net.focik.homeoffice.devices.domain.port.secondary;

import net.focik.homeoffice.devices.domain.model.Computer;
import net.focik.homeoffice.devices.domain.model.ComputerType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public interface ComputerRepository {
    <T extends Computer> T saveComputer(T computer);

    Optional<? extends Computer> findComputerById(int id, ComputerType type);

    List<? extends Computer> findAllComputers();

    List<? extends Computer> findComputersByUser(int userId);

    void deleteComputer(Integer id);
}
