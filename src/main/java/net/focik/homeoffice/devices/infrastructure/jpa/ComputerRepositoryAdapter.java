package net.focik.homeoffice.devices.infrastructure.jpa;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.devices.domain.model.Computer;
import net.focik.homeoffice.devices.domain.port.secondary.ComputerRepository;
import net.focik.homeoffice.devices.infrastructure.dto.ComputerDbDto;
import net.focik.homeoffice.devices.infrastructure.mapper.JpaComputerMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ComputerRepositoryAdapter implements ComputerRepository {

    private final ComputerDtoRepository computerDtoRepository;
    private final JpaComputerMapper mapper;


    @Override
    public Computer saveComputer(Computer computer) {
        ComputerDbDto dto = mapper.toDto(computer);
        log.debug("Saving computer: {}", dto);
        if (dto.getId() == 0) {
            dto.setId(null);
        }
        ComputerDbDto saved = computerDtoRepository.save(dto);
        log.debug("Saved computer: {}", saved);
        Computer domain = mapper.toDomain(dto);
        log.debug("Mapped saved computer to domain: {}", domain);
        return domain;
    }


    @Override
    public Optional<Computer> findComputerById(int id) {
        return computerDtoRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Computer> findAllComputers() {
        return computerDtoRepository.findAll().stream()
                .peek(dbDto -> log.debug("Found computer {}", dbDto))
                .map(mapper::toDomain)
                .peek(computer -> log.debug("Mapped computer {}", computer))
                .collect(Collectors.toList());
    }


    @Override
    public List<Computer> findComputersByUser(int userId) {
        return List.of();
    }

    @Override
    public void deleteComputer(Integer id) {
        computerDtoRepository.deleteById(id);
    }
}
