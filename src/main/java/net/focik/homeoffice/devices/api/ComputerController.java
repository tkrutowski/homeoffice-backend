package net.focik.homeoffice.devices.api;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.devices.api.dto.ComputerDto;
import net.focik.homeoffice.devices.api.mapper.ApiComputerMapper;
import net.focik.homeoffice.devices.domain.model.Computer;
import net.focik.homeoffice.devices.domain.port.primary.*;
import net.focik.homeoffice.utils.share.ActiveStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/computer")
public class ComputerController {
    private final FindComputerUseCase findComputerUseCase;
    private final SaveComputerUseCase saveComputerUseCase;
    private final ApiComputerMapper apiComputerMapper;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('COMPUTER_READ_ALL','COMPUTER_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<List<ComputerDto>> getComputersDevices(@RequestParam(value = "status", defaultValue = "ALL") ActiveStatus activeStatus) {
        log.info("Request to get computers with status: {}.",activeStatus);
        List<Computer> computers = findComputerUseCase.getComputers(activeStatus);

        if (computers.isEmpty()) {
            log.warn("No computers found.");
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        log.info("Found {} computers with status {}.", computers.size(), activeStatus);
        return new ResponseEntity<>(computers.stream()
                .peek(computer -> log.debug("Found computer {}", computer))
                .map(apiComputerMapper::toDto)
                .peek(computerDto -> log.debug("Mapped found computer {}", computerDto))
                .toList(), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('COMPUTER_READ_ALL','COMPUTER_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<ComputerDto> getById(@PathVariable int id) {
        log.info("Request to get computer by id: {}", id);
        Computer computer = findComputerUseCase.getComputerById(id);
        if (computer == null) {
            log.warn("No computer found with id: {}", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        log.info("Computer found: {}", computer);
        ComputerDto dto = apiComputerMapper.toDto(computer);
        log.debug("Mapped to Computer dto: {}", dto);
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('COMPUTER_WRITE_ALL','COMPUTER_WRITE') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<ComputerDto> addComputer(@RequestBody ComputerDto computerDto) {
        log.info("Request to add a new computer received with data: {}", computerDto);

        Computer computerToAdd = apiComputerMapper.toDomain(computerDto);
        log.debug("Mapped Computer DTO to domain object: {}", computerToAdd);

        Computer computerAdded = saveComputerUseCase.add(computerToAdd);
        log.info("Computer added successfully: {}", computerAdded);

        ComputerDto dto = apiComputerMapper.toDto(computerAdded);
        log.debug("Mapped Computer DTO to domain object: {}", dto);

        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @PutMapping()
    @PreAuthorize("hasAnyAuthority('COMPUTER_WRITE_ALL','COMPUTER_WRITE') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<ComputerDto> editComputer(@RequestBody ComputerDto computerDto) {
        log.info("Request to edit a computer received with data: {}", computerDto);

        Computer computerToUpdate = apiComputerMapper.toDomain(computerDto);
        log.debug("Mapped Device DTO to domain object: {}", computerToUpdate);

        Computer updatedComputer = saveComputerUseCase.update(computerToUpdate);
        log.info("Computer updated successfully: {}", updatedComputer);

        ComputerDto dto = apiComputerMapper.toDto(updatedComputer);
        log.debug("Mapped Computer DTO to domain object: {}", dto);

        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

//    @DeleteMapping("/{id}")
//    @PreAuthorize("hasAnyAuthority('COMPUTER_DELETE_ALL','COMPUTER_DELETE') or hasRole('ROLE_ADMIN')")
//    public void deleteComputer(@PathVariable Integer id) {
//        log.info("Request to delete computer with id: {}", id);
//        deleteDeviceUseCase.deleteDevice(id);
//        log.info("Device with id: {} deleted successfully", id);
//    }


    @PutMapping("/status/{id}")
    @PreAuthorize("hasAnyAuthority('COMPUTER_WRITE_ALL','COMPUTER_WRITE') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<ComputerDto> updateStatus(@PathVariable int id, @RequestParam ActiveStatus status) {
        log.info("Request to update status with id: {}", id);

        Computer updatedComputer = saveComputerUseCase.updateStatus(id, status);
        log.info("Computer updated successfully: {}", updatedComputer);

        ComputerDto dto = apiComputerMapper.toDto(updatedComputer);
        log.debug("Mapped Computer DTO to domain object: {}", dto);

        return new ResponseEntity<>(dto, HttpStatus.OK);
    }
}
