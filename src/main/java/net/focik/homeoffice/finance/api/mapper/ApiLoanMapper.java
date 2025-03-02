package net.focik.homeoffice.finance.api.mapper;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.finance.api.dto.LoanDto;
import net.focik.homeoffice.finance.api.dto.LoanInstallmentDto;
import net.focik.homeoffice.finance.domain.exception.LoanNotValidException;
import net.focik.homeoffice.finance.domain.loan.Loan;
import net.focik.homeoffice.finance.domain.loan.LoanInstallment;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.javamoney.moneta.Money;
import org.springframework.stereotype.Component;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ApiLoanMapper {

    private final ApiBankMapper bankMapper;

    public Loan toDomain(LoanDto dto) {
        valid(dto);
        return Loan.builder()
                .id(dto.getId())
                .bank(bankMapper.toDomain(dto.getBank()))
                .idUser(dto.getIdUser())
                .name(dto.getName())
                .amount(Money.of(BigDecimal.valueOf(dto.getAmount().doubleValue()), "PLN"))
                .date(dto.getDate())
                .loanNumber(dto.getLoanNumber())
                .accountNumber(dto.getAccountNumber())
                .firstPaymentDate(dto.getFirstPaymentDate())
                .numberOfInstallments(dto.getNumberOfInstallments())
                .installmentAmount(Money.of(BigDecimal.valueOf(dto.getInstallmentAmount().doubleValue()), "PLN"))
                .loanStatus(dto.getLoanStatus())
                .otherInfo(dto.getOtherInfo())
                .loanCost(Money.of(BigDecimal.valueOf(dto.getLoanCost().doubleValue()), "PLN"))
                .build();
    }

    public LoanDto toDto(Loan l) {
        return LoanDto.builder()
                .id(l.getId())
                .bank(bankMapper.toDto(l.getBank()))
                .idUser(l.getIdUser())
                .name(l.getName())
                .amount(l.getAmount().getNumber().doubleValue())
                .date(l.getDate())
                .loanNumber(l.getLoanNumber())
                .accountNumber(l.getAccountNumber())
                .firstPaymentDate(l.getFirstPaymentDate())
                .numberOfInstallments(l.getNumberOfInstallments())
                .installmentAmount(l.getInstallmentAmount().getNumber().doubleValue())
                .loanStatus(l.getLoanStatus())
                .loanCost(l.getLoanCost().getNumber().doubleValue())
                .otherInfo(l.getOtherInfo() == null ? "" : l.getOtherInfo())
                .installmentList(l.getInstallments() != null ? l.getInstallments()
                        .stream().map(this::toDto).collect(Collectors.toList()) : new ArrayList<>())
                .amountToPay(getAmountToPay(l.getInstallments()))
                .build();
    }

    private String getAmountToPay(List<LoanInstallment> loanInstallments) {
        CurrencyUnit currencyUnit = Monetary.getCurrency("PLN");
        if (loanInstallments != null) {
            Money reduce = loanInstallments.stream()
//                    .filter(loanInstallment -> loanInstallment.getPaymentStatus() == PaymentStatus.TO_PAY)
                    .filter(loanInstallment -> PaymentStatus.TO_PAY.equals(loanInstallment.getPaymentStatus()))
                    .map(LoanInstallment::getInstallmentAmountToPay)
                    .reduce((money, money2) -> money2.add(money))
                    .orElse(Money.zero(currencyUnit));
            long count = loanInstallments.stream()
                    .filter(loanInstallment -> loanInstallment.getPaymentStatus() == PaymentStatus.TO_PAY)
                    .count();
            return String.format("%.2f (%d)", reduce.getNumber().doubleValue(), count);
        }
        return "??? (?)";
    }

    public LoanInstallmentDto toDto(LoanInstallment l) {
        return LoanInstallmentDto.builder()
                .idLoanInstallment(l.getIdLoanInstallment())
                .idLoan(l.getIdLoan())
                .installmentNumber(l.getInstallmentNumber())
                .installmentAmountToPay(l.getInstallmentAmountToPay().getNumber().doubleValue())
                .installmentAmountPaid(l.getInstallmentAmountPaid().getNumber().doubleValue())
                .paymentDeadline(l.getPaymentDeadline())
                .paymentDate(l.getPaymentDate())
                .paymentStatus(l.getPaymentStatus())
                .build();
    }

    public LoanInstallment toDomain(LoanInstallmentDto dto) {
        valid(dto);
        return LoanInstallment.builder()
                .idLoanInstallment(dto.getIdLoanInstallment())
                .idLoan(dto.getIdLoan())
                .installmentNumber(dto.getInstallmentNumber())
                .installmentAmountToPay(Money.of(BigDecimal.valueOf(dto.getInstallmentAmountToPay().doubleValue()), "PLN"))
                .installmentAmountPaid(Money.of(BigDecimal.valueOf(dto.getInstallmentAmountPaid().doubleValue()), "PLN"))
                .paymentDeadline(dto.getPaymentDeadline())
                .paymentDate(dto.getPaymentDate())
                .paymentStatus(dto.getPaymentStatus())
                .build();
    }

    private void valid(LoanDto dto) {
        if (dto.getIdUser() == 0)
            throw new LoanNotValidException("IdUser can't be null.");
    }

    private void valid(LoanInstallmentDto dto) {
        if (dto.getIdLoan() == 0)
            throw new LoanNotValidException("IdLoan can't be null.");
    }
}