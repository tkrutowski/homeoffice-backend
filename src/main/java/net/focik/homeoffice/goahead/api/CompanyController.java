package net.focik.homeoffice.goahead.api;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.focik.homeoffice.goahead.api.dto.LookupResponseDto;
import net.focik.homeoffice.goahead.api.mapper.ApiLookupMapper;
import net.focik.homeoffice.goahead.domain.company.Company;
import net.focik.homeoffice.goahead.domain.company.LookupResponse;
import net.focik.homeoffice.goahead.domain.company.port.primary.GetCompanyUseCase;
import net.focik.homeoffice.goahead.domain.company.port.primary.LookupCompanyUseCase;
import net.focik.homeoffice.goahead.domain.company.port.primary.UpdateCompanyUseCase;
import net.focik.homeoffice.utils.exceptions.ExceptionHandling;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Log4j2
@RestController
@AllArgsConstructor
@RequestMapping("/api/goahead")
//@CrossOrigin
public class CompanyController extends ExceptionHandling {

    private final UpdateCompanyUseCase updateCompanyUseCase;
    private final GetCompanyUseCase getCompanyUseCase;
    private final LookupCompanyUseCase lookupCompanyUseCase;
    private final ApiLookupMapper lookupMapper;

    @GetMapping()
    @PreAuthorize("hasAnyAuthority('GOAHEAD_READ_ALL') or hasRole('ROLE_ADMIN')")
    ResponseEntity<Company> getCompanyDetails() {
        log.info("Request to get company details");
        Company company = getCompanyUseCase.get();
        if (company == null) {
            log.warn("No company found.");
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        log.info("Company found: {}", company);
        return new ResponseEntity<>(company, HttpStatus.OK);
    }


    @PutMapping
    @PreAuthorize("hasAnyAuthority('GOAHEAD_WRITE_ALL') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<Company> updateCompany(@RequestBody Company company) {
        log.info("Request to edit a company with data: {}", company);

        Company updatedCompany = updateCompanyUseCase.updateCompany(company);
        log.info("Company updated successfully: {}", updatedCompany);

        return new ResponseEntity<>(updatedCompany, HttpStatus.OK);
    }

    @GetMapping("/lookup")
    @PreAuthorize("hasAnyAuthority('GOAHEAD_READ_ALL') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<LookupResponseDto> lookupCompanyByNip(@RequestParam String nip) {
        log.info("Request to lookup company by NIP: {}", nip);
        String decodedNip = URLDecoder.decode(nip, StandardCharsets.UTF_8);
        
        LookupResponse lookupResponse = lookupCompanyUseCase.lookupByNip(decodedNip);
        if (lookupResponse == null) {
            log.warn("No company found for NIP: {}", decodedNip);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        log.info("Company lookup successful for NIP: {}", decodedNip);
        LookupResponseDto responseDto = lookupMapper.toDto(lookupResponse);
        return new ResponseEntity<>(responseDto, HttpStatus.OK);
    }

}
