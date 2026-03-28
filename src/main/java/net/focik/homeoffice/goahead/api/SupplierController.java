package net.focik.homeoffice.goahead.api;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.focik.homeoffice.goahead.api.dto.BasicDto;
import net.focik.homeoffice.goahead.api.dto.SupplierDto;
import net.focik.homeoffice.goahead.api.mapper.ApiSupplierMapper;
import net.focik.homeoffice.goahead.domain.customer.CustomerStatus;
import net.focik.homeoffice.goahead.domain.supplier.Supplier;
import net.focik.homeoffice.goahead.domain.supplier.port.primary.AddSupplierUseCase;
import net.focik.homeoffice.goahead.domain.supplier.port.primary.DeleteSupplierUseCase;
import net.focik.homeoffice.goahead.domain.supplier.port.primary.GetSupplierUseCase;
import net.focik.homeoffice.goahead.domain.supplier.port.primary.UpdateSupplierUseCase;
import net.focik.homeoffice.utils.exceptions.ExceptionHandling;
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
@RequestMapping("/api/goahead/supplier")
//@CrossOrigin
public class SupplierController extends ExceptionHandling {

    public static final String MAPPED_SUPPLIER_TO_SUPPLIER_DTO = "Mapped Supplier to SupplierDto: {}";
    public static final String MAPPED_SUPPLIER_DTO_TO_SUPPLIER_OBJECT = "Mapped SupplierDto to Supplier object: {}";
    private final ApiSupplierMapper mapper;
    private final AddSupplierUseCase addSupplierUseCase;
    private final UpdateSupplierUseCase updateSupplierUseCase;
    private final GetSupplierUseCase getSupplierUseCase;
    private final DeleteSupplierUseCase deleteSupplierUseCase;


    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('GOAHEAD_READ_ALL')")
    ResponseEntity<SupplierDto> getById(@PathVariable int id) {
        log.info("Request to get supplier by id: {}", id);

        Supplier supplier = getSupplierUseCase.findById(id);

        if (supplier == null) {
            log.warn("No supplier found with id: {}", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        log.info("Found supplier with id: {}", id);
        SupplierDto dto = mapper.toDto(supplier);
        log.debug(MAPPED_SUPPLIER_TO_SUPPLIER_DTO, dto);
        return new ResponseEntity<>(dto, OK);
    }

    @GetMapping()
    @PreAuthorize("hasAnyAuthority('GOAHEAD_READ_ALL')")
    ResponseEntity<List<SupplierDto>> getAllSuppliers(@RequestParam(required = false) CustomerStatus status) {
        log.info("Request to find all suppliers with status: {}", status);

        List<Supplier> supplierList = getSupplierUseCase.findByAll(status);
        log.info("Found {} suppliers.", supplierList.size());

        return new ResponseEntity<>(supplierList.stream()
                .peek(supplier -> log.debug("Found supplier {}", supplier))
                .map(mapper::toDto)
                .peek(dto -> log.debug("Mapped found supplier {}", dto))
                .collect(Collectors.toList()), OK);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('GOAHEAD_WRITE_ALL')")
    public ResponseEntity<SupplierDto> addSupplier(@RequestBody SupplierDto supplierDto) {
        log.info("Request to add a new supplier received with data: {}", supplierDto);

        Supplier supplierToAdd = mapper.toDomain(supplierDto);
        log.debug(MAPPED_SUPPLIER_DTO_TO_SUPPLIER_OBJECT, supplierToAdd);

        Supplier result = addSupplierUseCase.addSupplier(supplierToAdd);
        log.info("Supplier added successfully: {}", result);

        SupplierDto dto = mapper.toDto(result);
        log.debug(MAPPED_SUPPLIER_TO_SUPPLIER_DTO, dto);
        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('GOAHEAD_WRITE_ALL')")
    public ResponseEntity<SupplierDto> updateSupplier(@RequestBody SupplierDto supplierDto) {
        log.info("Request to edit a supplier received with data: {}", supplierDto);

        Supplier supplierToUpdate = mapper.toDomain(supplierDto);
        log.debug(MAPPED_SUPPLIER_DTO_TO_SUPPLIER_OBJECT, supplierToUpdate);

        Supplier supplier = updateSupplierUseCase.updateSupplier(supplierToUpdate);
        log.info("Supplier updated successfully: {}", supplier);

        SupplierDto dto = mapper.toDto(supplier);
        log.debug(MAPPED_SUPPLIER_TO_SUPPLIER_DTO, dto);
        return new ResponseEntity<>(dto, OK);
    }

    @DeleteMapping("/{idSupplier}")
    @PreAuthorize("hasAnyAuthority('GOAHEAD_DELETE_ALL')")
    public void deleteSupplier(@PathVariable int idSupplier) {
        log.info("Request to delete supplier with id: {}", idSupplier);
        deleteSupplierUseCase.deleteSupplier(idSupplier);
        log.info("Supplier with id: {} deleted successfully", idSupplier);
    }

    @PutMapping("/supplierstatus/{id}")
    @PreAuthorize("hasAnyAuthority('GOAHEAD_WRITE_ALL')")
    public void updateSupplierStatus(@PathVariable int id, @RequestBody BasicDto basicDto) {
        log.info("Request to update supplier status for supplier with id: {} from status: {}", id, basicDto.getValue());
        updateSupplierUseCase.updateSupplierStatus(id, CustomerStatus.valueOf(basicDto.getValue()));
        log.info("Supplier status updated successfully for supplier with id: {} to status: {}", id, basicDto.getValue());
    }
}
