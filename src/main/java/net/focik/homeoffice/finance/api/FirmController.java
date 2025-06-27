package net.focik.homeoffice.finance.api;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.focik.homeoffice.finance.api.dto.FirmDto;
import net.focik.homeoffice.finance.api.mapper.ApiFirmMapper;
import net.focik.homeoffice.finance.domain.firm.Firm;
import net.focik.homeoffice.finance.domain.firm.port.primary.AddFirmUseCase;
import net.focik.homeoffice.finance.domain.firm.port.primary.DeleteFirmUseCase;
import net.focik.homeoffice.finance.domain.firm.port.primary.GetFirmUseCase;
import net.focik.homeoffice.finance.domain.firm.port.primary.UpdateFirmUseCase;
import net.focik.homeoffice.utils.exceptions.ExceptionHandling;
import net.focik.homeoffice.utils.exceptions.HttpResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.OK;

@Log4j2
@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/finance/firm")
//@CrossOrigin
public class FirmController extends ExceptionHandling {

    public static final String MAPPED_TO_FIRM_DTO = "Mapped to Firm DTO: {}";
    public static final String MAPPED_FIRM_DTO_TO_DOMAIN_OBJECT = "Mapped Firm DTO to domain object: {}";
    private final AddFirmUseCase addFirmUseCase;
    private final UpdateFirmUseCase updateFirmUseCase;
    private final GetFirmUseCase getFirmUseCase;
    private final DeleteFirmUseCase deleteFirmUseCase;
    private final ApiFirmMapper apiFirmMapper;


    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('FINANCE_READ_ALL', 'FINANCE_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<FirmDto> getById(@PathVariable int id) {
        log.info("Request to get firm by id: {}", id);

        Firm firm = getFirmUseCase.findById(id);
        if (firm == null) {
            log.warn("No firm found with id: {}", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        log.info("Firm found: {}", firm);
        FirmDto dto = apiFirmMapper.toDto(firm);
        log.debug(MAPPED_TO_FIRM_DTO, dto);
        return new ResponseEntity<>(dto, OK);
    }

    @GetMapping()
    @PreAuthorize("hasAnyAuthority('ROLE_FINANCE', 'ROLE_ADMIN')")
    ResponseEntity<List<FirmDto>> getAll() {
        log.info("Request to get all firms");
        List<Firm> firmList = getFirmUseCase.findByAll();
        log.info("Found {} firms.", firmList.size());

        return new ResponseEntity<>(firmList.stream()
                .peek(firm -> log.debug("Found firm {}", firm))
                .map(apiFirmMapper::toDto)
                .peek(dto -> log.debug("Mapped found firm {}", dto))
                .collect(Collectors.toList()), OK);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('FINANCE_WRITE_ALL', 'FINANCE_WRITE') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<FirmDto> addFirm(@RequestBody FirmDto firmDto) {
        log.info("Request to add new firm: {}", firmDto);

        Firm firm = apiFirmMapper.toDomain(firmDto);
        log.debug(MAPPED_FIRM_DTO_TO_DOMAIN_OBJECT, firm);

        Firm result = addFirmUseCase.addFirm(firm);
        log.info("Firm added successfully: {}", result);

        FirmDto dto = apiFirmMapper.toDto(result);
        log.debug(MAPPED_TO_FIRM_DTO, dto);

        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('FINANCE_WRITE_ALL', 'FINANCE_WRITE', 'ROLE_ADMIN')")
    public ResponseEntity<FirmDto> updateFirm(@RequestBody FirmDto firmDto) {
        log.info("Request to update firm: {}", firmDto);

        Firm firm = apiFirmMapper.toDomain(firmDto);
        log.debug(MAPPED_FIRM_DTO_TO_DOMAIN_OBJECT, firm);

        Firm result = updateFirmUseCase.updateFirm(firm);
        log.info("Firm updated successfully: {}", result);

        FirmDto dto = apiFirmMapper.toDto(result);
        log.debug(MAPPED_TO_FIRM_DTO, dto);

        return new ResponseEntity<>(dto, OK);
    }

    @DeleteMapping("/{idFirm}")
    @PreAuthorize("hasAnyAuthority('FINANCE_DELETE_ALL', 'FINANCE_DELETE', 'ROLE_ADMIN')")
    public void deleteFirm(@PathVariable int idFirm) {
        log.info("Request to delete firm with id = {}", idFirm);
        deleteFirmUseCase.deleteFirm(idFirm);
        log.info("Firm with id = {} deleted successfully", idFirm);
    }
}
