package net.focik.homeoffice.finance.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.focik.homeoffice.finance.api.dto.BasicDto;
import net.focik.homeoffice.finance.api.dto.FeeDto;
import net.focik.homeoffice.finance.api.dto.FeeFrequencyDto;
import net.focik.homeoffice.finance.api.dto.FeeInstallmentDto;
import net.focik.homeoffice.finance.api.mapper.ApiFeeMapper;
import net.focik.homeoffice.finance.domain.fee.Fee;
import net.focik.homeoffice.finance.domain.fee.FeeFrequencyEnum;
import net.focik.homeoffice.finance.domain.fee.FeeInstallment;
import net.focik.homeoffice.finance.domain.fee.port.primary.AddFeeUseCase;
import net.focik.homeoffice.finance.domain.fee.port.primary.DeleteFeeUseCase;
import net.focik.homeoffice.finance.domain.fee.port.primary.GetFeeUseCase;
import net.focik.homeoffice.finance.domain.fee.port.primary.UpdateFeeUseCase;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.OK;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/finance/fee")
//@CrossOrigin
class FeeController {

    public static final String MAPPED_FEE_DTO_TO_DOMAIN_OBJECT = "Mapped Fee DTO to domain object: {}";
    public static final String MAPPED_TO_FEE_DTO = "Mapped to Fee DTO: {}";
    public static final String MAPPED_TO_FEE_INSTALLMENT_DTO = "Mapped to FeeInstallment DTO: {}";
    public static final String MAPPED_FEE_INSTALLMENT_DTO_TO_DOMAIN_OBJECT = "Mapped FeeInstallment DTO to domain object: {}";
    private final GetFeeUseCase getFeeUseCase;
    private final AddFeeUseCase addFeeUseCase;
    private final UpdateFeeUseCase updateFeeUseCase;
    private final DeleteFeeUseCase deleteFeeUseCase;
    private final ApiFeeMapper apiFeeMapper;


    //
    //LOAN
    //
    @GetMapping("/status")
    @PreAuthorize("hasAnyAuthority('FINANCE_FEE_READ', 'FINANCE_FEE_READ_ALL', 'ROLE_ADMIN')")
    ResponseEntity<List<FeeDto>> getFeeByStatus(@RequestParam(value = "status") PaymentStatus paymentStatus,
                                                @RequestParam(value = "installment", defaultValue = "false") boolean installment) {

        log.info("Request to get fees with status: {}", paymentStatus);

        List<Fee> feesByStatus = getFeeUseCase.getFeesByStatus(paymentStatus, installment);

        return new ResponseEntity<>(feesByStatus.stream()
                .peek(fee -> log.debug("Found fee {}", fee))
                .map(apiFeeMapper::toDto)
                .peek(dto -> log.debug("Mapped found fee {}", dto))
                .collect(Collectors.toList())
                , HttpStatus.OK);
    }

    @GetMapping("/{idFee}")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    ResponseEntity<FeeDto> getFeeById(@PathVariable int idFee) {

        log.info("Request to get fee by id: {}", idFee);

        Fee fee = getFeeUseCase.getFeeById(idFee, true);
        if (fee == null) {
            log.warn("No fee found with id: {}", idFee);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        log.info("Found fee {}", fee);
        FeeDto dto = apiFeeMapper.toDto(fee);
        log.debug(MAPPED_TO_FEE_DTO, dto);
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @GetMapping("/{idUser}/status")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    ResponseEntity<List<FeeDto>> getLoansByEmployeeAndStatus(@PathVariable int idUser,
                                                             @RequestParam(value = "paymentStatus") PaymentStatus paymentStatus,
                                                             @RequestParam(value = "installment") boolean installment) {

        log.info("Request to get fees for user id: {}", idUser);
        List<Fee> feesByUser = getFeeUseCase.getFeesByUser(idUser, paymentStatus, installment);
        log.info("Found {} fee(s) for user id: {}", feesByUser.size(), idUser);

        return new ResponseEntity<>(feesByUser.stream()
                .peek(fee -> log.debug("Found fee {}", fee))
                .map(apiFeeMapper::toDto)
                .peek(dto -> log.debug("Mapped found fee {}", dto))
                .collect(Collectors.toList())
                , HttpStatus.OK);
    }

    @GetMapping("/frequency")
    ResponseEntity<List<FeeFrequencyDto>> getFrequency() {
        FeeFrequencyEnum[] collect = (FeeFrequencyEnum.values());
        List<FeeFrequencyDto> statusDtos = Arrays.stream(collect)
                .map(type -> new FeeFrequencyDto(type.name(), type.getViewValue(), type.getFrequencyNumber()))
                .collect(Collectors.toList());
        return new ResponseEntity<>(statusDtos, OK);
    }

    @GetMapping("/page")
    ResponseEntity<Page<FeeDto>> getFeesPage(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", defaultValue = "date") String sortField,
            @RequestParam(name = "direction", defaultValue = "DESC") String sortDirection,
            @RequestParam(name = "globalFilter", required = false) String globalFilter,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "idFirm", required = false) Integer idFirm,
            @RequestParam(name = "idUser", required = false) Integer idUser,
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "dateComparisonType", required = false, defaultValue = "EQUALS") String dateComparisonType,
            @RequestParam(name = "amount", required = false) BigDecimal amount,
            @RequestParam(name = "amountComparisonType", required = false, defaultValue = "EQUALS") String amountComparisonType,
            @RequestParam(name = "status", required = false) PaymentStatus status
    ) {
        log.info("Request to get fees page with page: {}, size: {}, sort: {}, direction: {}, globalFilter: {}, idUser: {}, name: {}, firmId: {}, date: {}, dateComparisonType: {}, amount: {}, amountComparisonType: {}, status: {}",
                page, size, sortField, sortDirection, globalFilter, idUser, name, idFirm, date, dateComparisonType, amount, amountComparisonType, status);

        Page<Fee> feesPage = getFeeUseCase.findFeesPageableWithFilters(
                page, size, sortField, sortDirection, globalFilter, name, idFirm,
                date, dateComparisonType, amount, amountComparisonType, status, idUser);

        Page<FeeDto> dtoPage = feesPage.map(apiFeeMapper::toDto);

        log.debug("Found {} fees on page {} of {}",
                dtoPage.getNumberOfElements(),
                dtoPage.getNumber(),
                dtoPage.getTotalPages());

        return ResponseEntity.ok(dtoPage);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public ResponseEntity<FeeDto> addFee(@RequestBody FeeDto feeDto) {
        log.info("Request to add a new fee received with data: {}", feeDto);

        Fee feeToAdd = apiFeeMapper.toDomain(feeDto);
        log.debug(MAPPED_FEE_DTO_TO_DOMAIN_OBJECT, feeToAdd);

        Fee addedFee = addFeeUseCase.addFee(feeToAdd);
        log.debug("Fee added: {}", addedFee);

        FeeDto dto = apiFeeMapper.toDto(addedFee);
        log.debug(MAPPED_TO_FEE_DTO, dto);
        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public ResponseEntity<FeeDto> updateFee(@RequestBody FeeDto feeDto) {
        log.info("Request to edit a fee received with data: {}", feeDto);

        Fee fee = apiFeeMapper.toDomain(feeDto);
        log.debug(MAPPED_FEE_DTO_TO_DOMAIN_OBJECT, fee);

        Fee result = updateFeeUseCase.updateFee(fee);
        log.debug("Fee updated: {}", result);

        FeeDto dto = apiFeeMapper.toDto(result);
        log.debug(MAPPED_TO_FEE_DTO, dto);

        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }

    @PutMapping("/status/{id}")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public ResponseEntity<FeeDto> updateLFeeStatus(@PathVariable int id, @RequestBody BasicDto basicDto) {
        log.info("Request to edit a fee status received with data: {}", basicDto);

        Fee result = updateFeeUseCase.updateFeeStatus(id, PaymentStatus.valueOf(basicDto.getValue()));
        log.debug("Fee updated: {}", result);

        FeeDto dto = apiFeeMapper.toDto(result);
        log.debug(MAPPED_TO_FEE_DTO, dto);

        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @DeleteMapping("/{idFee}")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public void deleteFee(@PathVariable int idFee) {
        log.info("Request to delete fee with id: {}", idFee);
        deleteFeeUseCase.deleteFeeById(idFee);
        log.info("Fee with id: {} deleted successfully", idFee);
    }

    //-----------------------------------------------------------------------------------------------------------
    //FEE INSTALLMENT
    //
    @GetMapping("/installment/{idUser}/all")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    ResponseEntity<List<FeeInstallmentDto>> getFeeInstallmentByUserAndDate(@PathVariable int idUser,
                                                                           @RequestParam(value = "date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {

        log.info("Request to get all fee installment by date for user id: {}", idUser);

        List<FeeInstallment> feeInstallments = new ArrayList<>(getFeeUseCase.getFeeInstallments(idUser, date));
        log.info("Found {} fee installment(s) for user id: {}", feeInstallments.size(), idUser);

        return new ResponseEntity<>(feeInstallments.stream()
                .peek(feeInstallment -> log.debug("Found fee installment {}", feeInstallment))
                .map(apiFeeMapper::toDto)
                .peek(dto -> log.debug("Mapped found fee installment {}", dto))
                .collect(Collectors.toList())
                , HttpStatus.OK);
    }

    @GetMapping("/installment/{idFee}")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    ResponseEntity<List<FeeInstallmentDto>> getFeeInstallmentsByFee(@PathVariable int idFee) {
        log.info("Request to get all fee installment by fee id: {}", idFee);

        List<FeeInstallment> feeInstallments = getFeeUseCase.getFeeInstallments(idFee);
        log.info("Found {} fee installment(s) for fee id: {}", feeInstallments.size(), idFee);

        return new ResponseEntity<>(feeInstallments.stream()
                .peek(feeInstallment -> log.debug("Found fee installment {}", feeInstallment))
                .map(apiFeeMapper::toDto)
                .peek(dto -> log.debug("Mapped found fee installment {}", dto))
                .collect(Collectors.toList())
                , HttpStatus.OK);
    }

    @PostMapping("/installment")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public ResponseEntity<FeeInstallmentDto> addFeeInstallment(@RequestBody FeeInstallmentDto feeInstallmentDto) {
        log.info("Request to add a new FeeInstallment with data: {}", feeInstallmentDto);

        FeeInstallment feeInstallmentToAdd = apiFeeMapper.toDomain(feeInstallmentDto);
        log.debug(MAPPED_FEE_INSTALLMENT_DTO_TO_DOMAIN_OBJECT, feeInstallmentToAdd);

        FeeInstallment result = addFeeUseCase.addFeeInstallment(feeInstallmentToAdd);
        log.debug("Fee installment added: {}", result);

        FeeInstallmentDto dto = apiFeeMapper.toDto(result);
        log.debug(MAPPED_TO_FEE_INSTALLMENT_DTO, dto);

        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }

    @PutMapping("/installment")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public ResponseEntity<FeeInstallmentDto> updateFeeInstallment(@RequestBody FeeInstallmentDto feeInstallmentDto) {
        log.info("Request to edit a fee installment with data: {}", feeInstallmentDto);

        FeeInstallment feeInstallment = apiFeeMapper.toDomain(feeInstallmentDto);
        log.debug(MAPPED_FEE_INSTALLMENT_DTO_TO_DOMAIN_OBJECT, feeInstallment);

        FeeInstallment result = updateFeeUseCase.updateFeeInstallment(feeInstallment);
        log.debug("Fee installment updated: {}", result);

        log.info("Fee installment updated with id = " + result.getIdFeeInstallment());

        FeeInstallmentDto dto = apiFeeMapper.toDto(result);
        log.debug(MAPPED_TO_FEE_INSTALLMENT_DTO, dto);

        return new ResponseEntity<>(dto, OK);
    }

    @DeleteMapping("/installment/{idFeeInstallment}")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public void deleteFeeInstallment(@PathVariable int idFeeInstallment) {
        log.info("Request to delete fee installment with id: {}", idFeeInstallment);
        deleteFeeUseCase.deleteFeeInstallmentById(idFeeInstallment);
        log.info("Fee Installment with id: {} deleted successfully", idFeeInstallment);
    }
}