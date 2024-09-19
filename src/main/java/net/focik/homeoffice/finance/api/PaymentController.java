package net.focik.homeoffice.finance.api;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.focik.homeoffice.finance.api.dto.PaymentDto;
import net.focik.homeoffice.finance.api.mapper.ApiPaymentMapper;
import net.focik.homeoffice.finance.domain.payment.Payment;
import net.focik.homeoffice.finance.domain.payment.PaymentFacade;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Log4j2
@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/finance/payment")
//@CrossOrigin
class PaymentController {

    PaymentFacade paymentFacade;
    ApiPaymentMapper mapper;

    @GetMapping()
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    ResponseEntity<Map<Integer, List<PaymentDto>>> getPaymentsByYear(@RequestParam(value = "date", required = false) String date,
                                                                     @RequestParam(value = "status", defaultValue = "TO_PAY") PaymentStatus status) {
        int year = date == null ? LocalDate.now().getYear() : Integer.parseInt(date);

        log.info("Get payments for: " + year);

        Map<Integer, List<Payment>> paymentsByDate = paymentFacade.getPaymentsByDate(LocalDate.of(year, 1, 1), status);

        return new ResponseEntity<>(mapper.toDto(paymentsByDate), HttpStatus.OK);
    }
}