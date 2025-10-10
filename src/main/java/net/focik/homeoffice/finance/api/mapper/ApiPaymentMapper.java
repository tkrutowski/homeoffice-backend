package net.focik.homeoffice.finance.api.mapper;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.finance.api.dto.InstallmentDto;
import net.focik.homeoffice.finance.api.dto.PaymentDto;
import net.focik.homeoffice.finance.domain.fee.FeeInstallment;
import net.focik.homeoffice.finance.domain.loan.LoanInstallment;
import net.focik.homeoffice.finance.domain.payment.Installment;
import net.focik.homeoffice.finance.domain.payment.Payment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ApiPaymentMapper {

    private final ApiLoanMapper loanMapper;
    private final ApiFeeMapper feeMapper;

    public Map<Integer, List<PaymentDto>> toDto(Map<Integer, List<Payment>> paymentsMap) {
        Map<Integer, List<PaymentDto>> map = new HashMap<>();

        for (Map.Entry<Integer, List<Payment>> entry : paymentsMap.entrySet()) {
            List<PaymentDto> paymentDtos = entry.getValue()
                    .stream()
                    .map(this::toPaymentDto).collect(Collectors.toList());

            map.put(entry.getKey(), paymentDtos);
        }

        return map;
    }

    private PaymentDto toPaymentDto(Payment p) {
        return PaymentDto.builder()
                .id(p.getId())
                .idUser(p.getIdUser())
                .name(p.getName())
                .paymentDay(p.getPaymentDay())
                .paymentType(p.getPaymentType())
                .paymentStatus(p.getPaymentStatus())
                .installments(installmentToDto(p.getInstallments()))
                .build();
    }

    private List<? extends InstallmentDto> installmentToDto(List<? extends Installment> installments) {
        List<InstallmentDto> dtos = new ArrayList<>();

        for (Installment installment : installments) {
            if (installment instanceof LoanInstallment) {
                dtos.add(loanMapper.toDto((LoanInstallment) installment));
            }
            if (installment instanceof FeeInstallment) {
                dtos.add(feeMapper.toDto((FeeInstallment) installment));
            }
        }

        return dtos;
    }
}