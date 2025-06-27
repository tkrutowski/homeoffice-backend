package net.focik.homeoffice.goahead.api;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.focik.homeoffice.goahead.api.dto.BasicDto;
import net.focik.homeoffice.goahead.api.dto.CustomerDto;
import net.focik.homeoffice.goahead.api.mapper.ApiCustomerMapper;
import net.focik.homeoffice.goahead.domain.customer.Customer;
import net.focik.homeoffice.goahead.domain.customer.CustomerStatus;
import net.focik.homeoffice.goahead.domain.customer.CustomerType;
import net.focik.homeoffice.goahead.domain.customer.port.primary.AddCustomerUseCase;
import net.focik.homeoffice.goahead.domain.customer.port.primary.DeleteCustomerUseCase;
import net.focik.homeoffice.goahead.domain.customer.port.primary.GetCustomerUseCase;
import net.focik.homeoffice.goahead.domain.customer.port.primary.UpdateCustomerUseCase;
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
@RequestMapping("/api/goahead/customer")
//@CrossOrigin
public class CustomerController extends ExceptionHandling {

    public static final String MAPPED_CUSTOMER_TO_CUSTOMER_DTO = "Mapped Customer to CustomerDto: {}";
    public static final String MAPPED_CUSTOMER_DTO_TO_CUSTOMER_OBJECT = "Mapped CustomerDto to Customer object: {}";
    private final ApiCustomerMapper mapper;
    private final AddCustomerUseCase addCustomerUseCase;
    private final UpdateCustomerUseCase updateCustomerUseCase;
    private final GetCustomerUseCase getCustomerUseCase;
    private final DeleteCustomerUseCase deleteCustomerUseCase;

    @GetMapping("/test")
    @PreAuthorize("hasAnyAuthority('GOAHEAD_WRITE_ALL')")
    String test() {
        return "test";
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('GOAHEAD_READ_ALL')")
    ResponseEntity<CustomerDto> getById(@PathVariable int id) {
        log.info("Request to get customer by id: {}", id);

        Customer customer = getCustomerUseCase.findById(id);

        if (customer == null) {
            log.warn("No customer found with id: {}", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        log.info("Found customer with id: {}", id);
        CustomerDto dto = mapper.toDto(customer);
        log.debug(MAPPED_CUSTOMER_TO_CUSTOMER_DTO, dto);
        return new ResponseEntity<>(dto, OK);
    }

    @GetMapping()
    @PreAuthorize("hasAnyAuthority('GOAHEAD_READ_ALL')")
    ResponseEntity<List<CustomerDto>> getAllCustomers(@RequestParam(required = false) CustomerStatus status,
                                                      @RequestParam(required = false) CustomerType type) {
        log.info("Request to find all employees with status: {} and type: {}", status, type);

        List<Customer> customerList = getCustomerUseCase.findByAll(status, type);
        log.info("Found {} employees.", customerList.size());

        return new ResponseEntity<>(customerList.stream()
                .peek(customer -> log.debug("Found customer {}", customer))
                .map(mapper::toDto)
                .peek(dto -> log.debug("Mapped found customer {}", dto))
                .collect(Collectors.toList()), OK);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('GOAHEAD_WRITE_ALL')")
    public ResponseEntity<CustomerDto> addCustomer(@RequestBody CustomerDto customerDto) {
        log.info("Request to add a new customer received with data: {}", customerDto);

        Customer customerToAdd = mapper.toDomain(customerDto);
        log.debug(MAPPED_CUSTOMER_DTO_TO_CUSTOMER_OBJECT, customerToAdd);

        Customer result = addCustomerUseCase.addCustomer(customerToAdd);
        log.info("Customer added successfully: {}", result);

        CustomerDto dto = mapper.toDto(result);
        log.debug(MAPPED_CUSTOMER_TO_CUSTOMER_DTO, dto);
        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('GOAHEAD_WRITE_ALL')")
    public ResponseEntity<CustomerDto> updateEmployee(@RequestBody CustomerDto customerDto) {
        log.info("Request to edit a customer received with data: {}", customerDto);

        Customer customerToUpdate = mapper.toDomain(customerDto);
        log.debug(MAPPED_CUSTOMER_DTO_TO_CUSTOMER_OBJECT, customerToUpdate);

        Customer customer = updateCustomerUseCase.updateCustomer(customerToUpdate);
        log.info("Customer updated successfully: {}", customer);

        CustomerDto dto = mapper.toDto(customer);
        log.debug(MAPPED_CUSTOMER_TO_CUSTOMER_DTO, dto);
        return new ResponseEntity<>(dto, OK);
    }

    @DeleteMapping("/{idCustomer}")
    @PreAuthorize("hasAnyAuthority('GOAHEAD_DELETE_ALL')")
    public void deleteCustomer(@PathVariable int idCustomer) {
        log.info("Request to delete customer with id: {}", idCustomer);
        deleteCustomerUseCase.deleteCustomer(idCustomer);
        log.info("Customer with id: {} deleted successfully", idCustomer);
    }

    @GetMapping("/customertype")
    ResponseEntity<List<CustomerType>> getCustomerTypes() {
        log.info("Request to retrieve customer types.");
        List<CustomerType> types = List.of(CustomerType.values());
        log.info("Retrieved {} customer types.", types.size());
        return new ResponseEntity<>(types, OK);
    }

    @PutMapping("/customerstatus/{id}")
    @PreAuthorize("hasAnyAuthority('GOAHEAD_WRITE_ALL')")
    public void updateCustomerStatus(@PathVariable int id, @RequestBody BasicDto basicDto) {
        log.info("Request to update customer status for employee with id: {} from status: {}", id, basicDto.getValue());
        updateCustomerUseCase.updateCustomerStatus(id, CustomerStatus.valueOf(basicDto.getValue()));
        log.info("Customer status updated successfully for employee with id: {} to status: {}", id, basicDto.getValue());
    }
}
