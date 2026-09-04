package net.focik.homeoffice.devices.infrastructure.jpa;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.devices.domain.model.Computer;
import net.focik.homeoffice.devices.domain.model.ComputerType;
import net.focik.homeoffice.devices.domain.model.DesktopComputer;
import net.focik.homeoffice.devices.domain.model.LaptopComputer;
import net.focik.homeoffice.devices.domain.port.secondary.ComputerRepository;
import net.focik.homeoffice.devices.infrastructure.dto.DesktopComputerDbDto;
import net.focik.homeoffice.devices.infrastructure.dto.LaptopComputerDbDto;
import net.focik.homeoffice.devices.infrastructure.mapper.JpaDesktopComputerMapper;
import net.focik.homeoffice.devices.infrastructure.mapper.JpaLaptopComputerMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ComputerRepositoryAdapter implements ComputerRepository {

    private final DesktopComputerDtoRepository desktopRepository;
    private final LaptopComputerDtoRepository laptopRepository;
    private final JpaDesktopComputerMapper desktopMapper;
    private final JpaLaptopComputerMapper laptopMapper;


    @Override
    public <T extends Computer> T saveComputer(T computer) {
        if (computer instanceof DesktopComputer desktop) {
            DesktopComputerDbDto dto = desktopMapper.toDto(desktop);
            log.debug("Saving desktop computer: {}", dto);
            if (dto.getId() == 0) {
                dto.setId(null);
            }
            DesktopComputerDbDto saved = desktopRepository.save(dto);
            log.debug("Saved desktop computer: {}", saved);
            @SuppressWarnings("unchecked")
            T domain = (T) desktopMapper.toDomain(saved);
            log.debug("Mapped saved desktop computer to domain: {}", domain);
            return domain;
        } else if (computer instanceof LaptopComputer laptop) {
            LaptopComputerDbDto dto = laptopMapper.toDto(laptop);
            log.debug("Saving laptop computer: {}", dto);
            if (dto.getId() == 0) {
                dto.setId(null);
            }
            LaptopComputerDbDto saved = laptopRepository.save(dto);
            log.debug("Saved laptop computer: {}", saved);
            @SuppressWarnings("unchecked")
            T domain = (T) laptopMapper.toDomain(saved);
            log.debug("Mapped saved laptop computer to domain: {}", domain);
            return domain;
        } else {
            throw new IllegalArgumentException("Unknown computer type: " + computer.getClass().getName());
        }
    }


    @Override
    public Optional<? extends Computer> findComputerById(int id, ComputerType type) {
        if (type == ComputerType.DESKTOP) {
            Optional<DesktopComputerDbDto> desktopOpt = desktopRepository.findById(id);
            log.debug("Finding desktop computer with id: {} - Present: {}", id, desktopOpt.isPresent());
            return desktopOpt.map(desktopMapper::toDomain);
        } else if (type == ComputerType.LAPTOP) {
            Optional<LaptopComputerDbDto> laptopOpt = laptopRepository.findById(id);
            log.debug("Finding laptop computer with id: {} - Present: {}", id, laptopOpt.isPresent());
            return laptopOpt.map(laptopMapper::toDomain);
        } else {
            log.warn("Unsupported computer type: {}", type);
            return Optional.empty();
        }
    }

    @Override
    public List<? extends Computer> findAllComputers() {
        List<Computer> computers = new ArrayList<>();

        desktopRepository.findAll().stream()
                .peek(dbDto -> log.debug("Found desktop computer {}", dbDto))
                .map(desktopMapper::toDomain)
                .peek(computer -> log.debug("Mapped desktop computer {}", computer))
                .forEach(computers::add);

        laptopRepository.findAll().stream()
                .peek(dbDto -> log.debug("Found laptop computer {}", dbDto))
                .map(laptopMapper::toDomain)
                .peek(computer -> log.debug("Mapped laptop computer {}", computer))
                .forEach(computers::add);

        return computers;
    }


    @Override
    public List<? extends Computer> findComputersByUser(int userId) {
        List<Computer> computers = new ArrayList<>();

        desktopRepository.findAll().stream()
                .filter(dto -> dto.getIdUser() == userId)
                .map(desktopMapper::toDomain)
                .forEach(computers::add);

        laptopRepository.findAll().stream()
                .filter(dto -> dto.getIdUser() == userId)
                .map(laptopMapper::toDomain)
                .forEach(computers::add);

        return computers;
    }

    @Override
    public void deleteComputer(Integer id) {
        desktopRepository.deleteById(id);
        laptopRepository.deleteById(id);
    }
}
