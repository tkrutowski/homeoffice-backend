package net.focik.homeoffice.finance.api.mapper;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.finance.api.dto.FeeDto;
import net.focik.homeoffice.finance.api.dto.FeeFrequencyDto;
import net.focik.homeoffice.finance.api.dto.FeeInstallmentDto;
import net.focik.homeoffice.finance.domain.exception.FeeNotValidException;
import net.focik.homeoffice.finance.domain.fee.Fee;
import net.focik.homeoffice.finance.domain.fee.FeeFrequencyEnum;
import net.focik.homeoffice.finance.domain.fee.FeeInstallment;
import org.javamoney.moneta.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ApiFeeMapper {

    public Fee toDomain(FeeDto dto) {
        valid(dto);
        return Fee.builder()
                .id(dto.getId())
                .firm(ApiFirmMapper.toDomain(dto.getFirm()))
                .idUser(dto.getIdUser())
                .name(dto.getName())
                .feeNumber(dto.getFeeNumber())
                .date(dto.getDate())
                .feeFrequency(FeeFrequencyEnum.valueOf(dto.getFeeFrequency().getName()))
                .numberOfPayments(dto.getNumberOfPayments())
                .amount(Money.of(BigDecimal.valueOf(dto.getAmount().doubleValue()), "PLN"))
                .firstPaymentDate(dto.getFirstPaymentDate())
                .accountNumber(dto.getAccountNumber())
                .feeStatus(dto.getFeeStatus())
                .otherInfo(dto.getOtherInfo())
                .build();
    }

    public FeeDto toDto(Fee fee) {
        return FeeDto.builder()
                .id(fee.getId())
                .firm(ApiFirmMapper.toDto(fee.getFirm()))
                .idUser(fee.getIdUser())
                .name(fee.getName())
                .feeNumber(fee.getFeeNumber())
                .date(fee.getDate())
                .feeFrequency(new FeeFrequencyDto(fee.getFeeFrequency().toString(), fee.getFeeFrequency().getViewValue(), fee.getFeeFrequency().getFrequencyNumber()))
                .numberOfPayments(fee.getNumberOfPayments())
                .amount(fee.getAmount().getNumber().doubleValue())
                .firstPaymentDate(fee.getFirstPaymentDate())
                .accountNumber(fee.getAccountNumber())
                .feeStatus(fee.getFeeStatus())
                .otherInfo(fee.getOtherInfo() == null ? "" : fee.getOtherInfo())
                .installmentList(fee.getInstallments() != null ? fee.getInstallments()
                        .stream().map(this::toDto).collect(Collectors.toList()) : new ArrayList<>())
                .build();
    }

    public FeeInstallmentDto toDto(FeeInstallment fee) {
        return FeeInstallmentDto.builder()
                .idFeeInstallment(fee.getIdFeeInstallment())
                .idFee(fee.getIdFee())
                .installmentAmountToPay(fee.getInstallmentAmountToPay().getNumber().doubleValue())
                .installmentAmountPaid(fee.getInstallmentAmountPaid().getNumber().doubleValue())
                .paymentDeadline(fee.getPaymentDeadline())
                .paymentDate(fee.getPaymentDate())
                .paymentStatus(fee.getPaymentStatus())
                .build();
    }

    public FeeInstallment toDomain(FeeInstallmentDto dto) {
        valid(dto);
        return FeeInstallment.builder()
                .idFeeInstallment(dto.getIdFeeInstallment())
                .idFee(dto.getIdFee())
                .installmentAmountToPay(Money.of(BigDecimal.valueOf(dto.getInstallmentAmountToPay().doubleValue()), "PLN"))
                .installmentAmountPaid(Money.of(BigDecimal.valueOf(dto.getInstallmentAmountPaid().doubleValue()), "PLN"))
                .paymentDeadline(dto.getPaymentDeadline())
                .paymentDate(dto.getPaymentDate())
                .paymentStatus(dto.getPaymentStatus())
                .build();
    }

    private void valid(FeeDto dto) {
        if (dto.getIdUser() == 0)
            throw new FeeNotValidException("IdUser can't be null.");
        if (dto.getDate() == null)
            throw new FeeNotValidException("Date can't be empty.");
    }

    private void valid(FeeInstallmentDto dto) {
        if (dto.getIdFee() == 0)
            throw new FeeNotValidException("IdFee can't be null.");
    }
}