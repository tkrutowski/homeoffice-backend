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

    private final AddFirmUseCase addFirmUseCase;
    private final UpdateFirmUseCase updateFirmUseCase;
    private final GetFirmUseCase getFirmUseCase;
    private final DeleteFirmUseCase deleteFirmUseCase;
    private final ApiFirmMapper apiFirmMapper;


    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('FINANCE_READ_ALL', 'FINANCE_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<FirmDto> getById(@PathVariable int id) {

        log.info("Try find firm by id: " + id);

        Firm firm = getFirmUseCase.findById(id);

        log.info(firm != null ? "Found firm for id = " + id : "Not found firm for id = " + id);

        if (firm == null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        return new ResponseEntity<>(apiFirmMapper.toDto(firm), OK);
    }

    @GetMapping()
    @PreAuthorize("hasAnyAuthority('ROLE_FINANCE', 'ROLE_ADMIN')")
    ResponseEntity<List<FirmDto>> getAll() {
        log.info("Try get all firms ");

        List<Firm> firmList = getFirmUseCase.findByAll();

        log.info("Found " + firmList.size() + " firms.");

        return new ResponseEntity<>(firmList.stream()
                .map(apiFirmMapper::toDto)
                .collect(Collectors.toList()), OK);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('FINANCE_WRITE_ALL', 'FINANCE_WRITE') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<FirmDto> addFirm(@RequestBody FirmDto firmDto) {
        log.info("Request to add new firm: {}", firmDto);

        Firm firm = apiFirmMapper.toDomain(firmDto);
        Firm result = addFirmUseCase.addFirm(firm);

        if (result.getId() <= 0) {
            log.warn("No firm was added!");
            return ResponseEntity.badRequest().build();
        }

        log.info("Firm added successfully with ID = {}", result.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(apiFirmMapper.toDto(result));
    }


    @PutMapping
    @PreAuthorize("hasAnyAuthority('FINANCE_WRITE_ALL', 'FINANCE_WRITE', 'ROLE_ADMIN')")
    public ResponseEntity<FirmDto> updateFirm(@RequestBody FirmDto firmDto) {
        log.info("Try update firm with id: {}", firmDto.getId());

        Firm firm = updateFirmUseCase.updateFirm(apiFirmMapper.toDomain(firmDto));
        return new ResponseEntity<>(apiFirmMapper.toDto(firm), OK);
    }

    @DeleteMapping("/{idFirm}")
    @PreAuthorize("hasAnyAuthority('FINANCE_DELETE_ALL', 'FINANCE_DELETE', 'ROLE_ADMIN')")
    public ResponseEntity<HttpResponse> deleteFirm(@PathVariable int idFirm) {
        log.info("Try delete firm with id: " + idFirm);

        deleteFirmUseCase.deleteFirm(idFirm);

        log.info("Deleted firm with id = " + idFirm);

        return response(HttpStatus.NO_CONTENT, "Firma usunięty.");
    }

    private ResponseEntity<HttpResponse> response(HttpStatus status, String message) {
        HttpResponse body = new HttpResponse(status.value(), status, status.getReasonPhrase(), message);
        return new ResponseEntity<>(body, status);
    }
}
