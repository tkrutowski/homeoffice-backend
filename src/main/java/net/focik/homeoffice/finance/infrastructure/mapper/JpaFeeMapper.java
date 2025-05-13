package net.focik.homeoffice.finance.infrastructure.mapper;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.finance.domain.fee.Fee;
import net.focik.homeoffice.finance.domain.fee.FeeInstallment;
import net.focik.homeoffice.finance.domain.firm.Firm;
import net.focik.homeoffice.finance.infrastructure.dto.FeeDbDto;
import net.focik.homeoffice.finance.infrastructure.dto.FeeInstallmentDbDto;
import net.focik.homeoffice.finance.infrastructure.dto.FirmDbDto;
import org.javamoney.moneta.Money;
import org.javamoney.moneta.spi.MoneyUtils;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JpaFeeMapper {
    private final ModelMapper mapper;
    public FeeDbDto toDto(Fee fee) {
        return FeeDbDto.builder()
                .id(fee.getId() == 0 ? null : fee.getId())
                .firm(mapper.map(fee.getFirm(), FirmDbDto.class))
                .idUser(fee.getIdUser())
                .name(fee.getName())
                .feeNumber(fee.getFeeNumber())
                .date(fee.getDate())
                .feeFrequencyEnum(fee.getFeeFrequency())
                .numberOfPayments(fee.getNumberOfPayments())
                .amount(MoneyUtils.getBigDecimal(fee.getAmount().getNumber()))
                .firstPaymentDate(fee.getFirstPaymentDate())
                .accountNumber(fee.getAccountNumber())
                .otherInfo(fee.getOtherInfo())
                .feeStatus(fee.getFeeStatus())
                .build();
    }

    public Fee toDomain(FeeDbDto dto, List<FeeInstallmentDbDto> feeInstallments) {
        Fee fee = toDomain(dto);
        fee.setInstallments(mapListFeeInstallmentToSet(feeInstallments));
        return fee;
    }

    public Fee toDomain(FeeDbDto dto) {
        return Fee.builder()
                .id(dto.getId())
                .firm(mapper.map(dto.getFirm(), Firm.class))
                .idUser(dto.getIdUser())
                .name(dto.getName())
                .feeNumber(dto.getFeeNumber())
                .date(dto.getDate())
                .feeFrequency(dto.getFeeFrequencyEnum())
                .numberOfPayments(dto.getNumberOfPayments())
                .amount(Money.of(dto.getAmount(),"PLN"))
                .firstPaymentDate(dto.getFirstPaymentDate())
                .accountNumber(dto.getAccountNumber())
                .feeStatus(dto.getFeeStatus())
                .otherInfo(dto.getOtherInfo())
                .build();
    }

    private List<FeeInstallment> mapListFeeInstallmentToSet(List<FeeInstallmentDbDto> installmentDtoList) {
        List<FeeInstallment> feeInstallmentSet = new ArrayList<>();

        installmentDtoList.stream()
                .map(this::toDomain)
                .forEach(feeInstallmentSet::add);

        return feeInstallmentSet;
    }

    public FeeInstallmentDbDto toDto(FeeInstallment feeInstallment) {
        return FeeInstallmentDbDto.builder()
                .id(feeInstallment.getIdFeeInstallment())
                .idFee(feeInstallment.getIdFee())
                .installmentAmountToPay(MoneyUtils.getBigDecimal(feeInstallment.getInstallmentAmountToPay().getNumber()))
                .installmentAmountPaid(MoneyUtils.getBigDecimal(feeInstallment.getInstallmentAmountPaid().getNumber()))
                .paymentDeadline(feeInstallment.getPaymentDeadline())
                .paymentDate(feeInstallment.getPaymentDate())
                .paymentStatus(feeInstallment.getPaymentStatus())
                .build();
    }

    public FeeInstallment toDomain(FeeInstallmentDbDto dto) {
        return FeeInstallment.builder()
                .idFeeInstallment(dto.getId())
                .idFee(dto.getIdFee())
                .installmentAmountToPay(Money.of(dto.getInstallmentAmountToPay(),"PLN"))
                .installmentAmountPaid(Money.of(dto.getInstallmentAmountPaid(),"PLN"))
                .paymentDeadline(dto.getPaymentDeadline())
                .paymentDate(dto.getPaymentDate())
                .paymentStatus(dto.getPaymentStatus())
                .build();
    }
}
