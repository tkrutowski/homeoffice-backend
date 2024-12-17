package net.focik.homeoffice.finance.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.focik.homeoffice.finance.api.dto.*;
import net.focik.homeoffice.finance.api.mapper.ApiFeeMapper;
import net.focik.homeoffice.finance.domain.fee.Fee;
import net.focik.homeoffice.finance.domain.fee.FeeFrequencyEnum;
import net.focik.homeoffice.finance.domain.fee.FeeInstallment;
import net.focik.homeoffice.finance.domain.fee.port.primary.AddFeeUseCase;
import net.focik.homeoffice.finance.domain.fee.port.primary.DeleteFeeUseCase;
import net.focik.homeoffice.finance.domain.fee.port.primary.GetFeeUseCase;
import net.focik.homeoffice.finance.domain.fee.port.primary.UpdateFeeUseCase;
import net.focik.homeoffice.utils.exceptions.HttpResponse;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

        log.info("Get fee with status: " + paymentStatus);

        List<Fee> feesByStatus = getFeeUseCase.getFeesByStatus(paymentStatus, installment);

        return new ResponseEntity<>(feesByStatus.stream()
                .map(apiFeeMapper::toDto)
                .collect(Collectors.toList())
                , HttpStatus.OK);
    }

    @GetMapping("/{idFee}")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    ResponseEntity<FeeDto> getFeeById(@PathVariable int idFee) {

        log.info("Get fee by id: " + idFee);

        Fee fee = getFeeUseCase.getFeeById(idFee, true);

        return new ResponseEntity<>(apiFeeMapper.toDto(fee), HttpStatus.OK);
    }

    @GetMapping("/{idUser}/status")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    ResponseEntity<List<FeeDto>> getLoansByEmployeeAndStatus(@PathVariable int idUser,
                                                              @RequestParam(value = "paymentStatus") PaymentStatus paymentStatus,
                                                              @RequestParam(value = "installment") boolean installment) {

        log.info("Get fees for user id: " + idUser);

        List<Fee> feesByUser = getFeeUseCase.getFeesByUser(idUser, paymentStatus, installment);

        return new ResponseEntity<>(feesByUser.stream()
                .map(apiFeeMapper::toDto)
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

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public ResponseEntity<FeeDto> addFee(@RequestBody FeeDto feeDto) {
        log.info("Try add new fee.");

        Fee fee = apiFeeMapper.toDomain(feeDto);
        Fee result = addFeeUseCase.addFee(fee);

        log.info("Fee added with id = " + result.getId());

        return new ResponseEntity<>(apiFeeMapper.toDto(result), HttpStatus.CREATED);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public ResponseEntity<FeeDto> updateFee(@RequestBody FeeDto feeDto) {
        log.info("Try update fee.");

        Fee fee = apiFeeMapper.toDomain(feeDto);
        Fee result = updateFeeUseCase.updateFee(fee);

        log.info("Fee updated with id = " + result.getId());

        return new ResponseEntity<>(apiFeeMapper.toDto(result), HttpStatus.CREATED);
    }

    @PutMapping("/status/{id}")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public ResponseEntity<FeeDto> updateLFeeStatus(@PathVariable int id, @RequestBody BasicDto basicDto) {
        log.info("Try update fee status.");

        Fee result = updateFeeUseCase.updateFeeStatus(id, PaymentStatus.valueOf(basicDto.getValue()));
        return new ResponseEntity<>(apiFeeMapper.toDto(result), HttpStatus.OK);
    }

    @DeleteMapping("/{idFee}")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public ResponseEntity<HttpResponse> deleteFee(@PathVariable int idFee) {
        log.info("Try to delete fee with id: " + idFee);

        deleteFeeUseCase.deleteFeeById(idFee);

        log.info("Deleted fee with id = " + idFee);

        return response(HttpStatus.NO_CONTENT, "Opłata usunięta.");
    }

    //-----------------------------------------------------------------------------------------------------------
    //FEE INSTALLMENT
    //
    @GetMapping("/installment/{idUser}/all")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    ResponseEntity<List<FeeInstallmentDto>> getFeeInstallmentByUserAndDate(@PathVariable int idUser,
                                                                            @RequestParam(value = "date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {

        log.info("Get all fee installment by date for user id: " + idUser);

        List<FeeInstallment> feeInstallments = new ArrayList<>(getFeeUseCase.getFeeInstallments(idUser, date));

        return new ResponseEntity<>(feeInstallments.stream()
                .map(apiFeeMapper::toDto)
                .collect(Collectors.toList())
                , HttpStatus.OK);
    }

    @GetMapping("/installment/{idFee}")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    ResponseEntity<List<FeeInstallmentDto>> getFeeInstallmentsByLoan(@PathVariable int idFee) {

        log.info("Get all fee installment by loan id: " + idFee);

        List<FeeInstallment> feeInstallments = getFeeUseCase.getFeeInstallments(idFee);

        return new ResponseEntity<>(feeInstallments.stream()
                .map(apiFeeMapper::toDto)
                .collect(Collectors.toList())
                , HttpStatus.OK);
    }

    @PostMapping("/installment")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public ResponseEntity<FeeInstallmentDto> addFeeInstallment(@RequestBody FeeInstallmentDto feeInstallmentDto) {
        log.info("Try add new fee installment.");

        FeeInstallment feeInstallment = apiFeeMapper.toDomain(feeInstallmentDto);
        FeeInstallment result = addFeeUseCase.addFeeInstallment(feeInstallment);

        log.info("Fee installment added with id = " + result.getIdFeeInstallment());

        return new ResponseEntity<>(apiFeeMapper.toDto(result), HttpStatus.CREATED);
    }

    @PutMapping("/installment")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public ResponseEntity<FeeInstallmentDto> updateFeeInstallment(@RequestBody FeeInstallmentDto feeInstallmentDto) {
        log.info("Try update fee installment.");

        FeeInstallment feeInstallment = apiFeeMapper.toDomain(feeInstallmentDto);
        FeeInstallment result = updateFeeUseCase.updateFeeInstallment(feeInstallment);

        log.info("Fee installment updated with id = " + result.getIdFeeInstallment());

        return new ResponseEntity<>(apiFeeMapper.toDto(result), HttpStatus.CREATED);
    }

    @DeleteMapping("/installment/{idFeeInstallment}")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public ResponseEntity<HttpResponse> deleteFeeInstallment(@PathVariable int idFeeInstallment) {
        log.info("Try to delete fee installment with id: " + idFeeInstallment);

        deleteFeeUseCase.deleteFeeInstallmentById(idFeeInstallment);

        log.info("Deleted fee with id = " + idFeeInstallment);

        return response(HttpStatus.NO_CONTENT, "Pożyczka usunięta.");
    }

    private ResponseEntity<HttpResponse> response(HttpStatus status, String message) {
        HttpResponse body = new HttpResponse(status.value(), status, status.getReasonPhrase(), message);
        return new ResponseEntity<>(body, status);
    }
}