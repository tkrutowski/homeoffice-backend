package net.focik.homeoffice.finance.infrastructure.mapper;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.finance.domain.bank.Bank;
import net.focik.homeoffice.finance.domain.loan.Loan;
import net.focik.homeoffice.finance.domain.loan.LoanInstallment;
import net.focik.homeoffice.finance.infrastructure.dto.BankDbDto;
import net.focik.homeoffice.finance.infrastructure.dto.LoanDbDto;
import net.focik.homeoffice.finance.infrastructure.dto.LoanInstallmentDbDto;
import org.javamoney.moneta.Money;
import org.javamoney.moneta.spi.MoneyUtils;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JpaLoanMapper {
    private final ModelMapper mapper;

    public LoanDbDto toDto(Loan loan) {
        return LoanDbDto.builder()
                .id(loan.getId() == 0 ? null : loan.getId())
                .bank(mapper.map(loan.getBank(), BankDbDto.class))
                .idUser(loan.getIdUser())
                .amount(MoneyUtils.getBigDecimal(loan.getAmount().getNumber()))
                .date(loan.getDate())
                .loanNumber(loan.getLoanNumber())
                .accountNumber(loan.getAccountNumber())
                .firstPaymentDate(loan.getFirstPaymentDate())
                .numberOfInstallments(loan.getNumberOfInstallments())
                .installmentAmount(MoneyUtils.getBigDecimal(loan.getInstallmentAmount().getNumber()))
                .name(loan.getName())
                .otherInfo(loan.getOtherInfo())
                .loanStatus(loan.getLoanStatus())
                .loanCost(MoneyUtils.getBigDecimal(loan.getLoanCost().getNumber()))
                .build();
    }

    public Loan toDomain(LoanDbDto dto, List<LoanInstallmentDbDto> loanInstallments) {
        Loan loan = toDomain(dto);
        loan.setInstallments(mapListLoanInstallmentToSet(loanInstallments));
        return loan;
    }

    public Loan toDomain(LoanDbDto dto) {
        return Loan.builder()
                .id(dto.getId())
                .bank(mapper.map(dto.getBank(), Bank.class))
                .idUser(dto.getIdUser())
                .name(dto.getName())
                .amount(Money.of(dto.getAmount(),"PLN"))
                .date(dto.getDate())
                .loanNumber(dto.getLoanNumber())
                .accountNumber(dto.getAccountNumber())
                .firstPaymentDate(dto.getFirstPaymentDate())
                .numberOfInstallments(dto.getNumberOfInstallments())
                .installmentAmount(Money.of(dto.getInstallmentAmount(),"PLN"))
                .loanStatus(dto.getLoanStatus())
                .loanCost(Money.of(dto.getLoanCost(),"PLN"))
                .otherInfo(dto.getOtherInfo())
                .build();
    }

    private List<LoanInstallment> mapListLoanInstallmentToSet(List<LoanInstallmentDbDto> installmentDtoList) {
        List<LoanInstallment> loanInstallmentSet = new ArrayList<>();

        installmentDtoList.stream()
                .map(this::toDomain)
                .forEach(loanInstallmentSet::add);

        return loanInstallmentSet;
    }

    public LoanInstallmentDbDto toDto(LoanInstallment loanInstallment) {
        return LoanInstallmentDbDto.builder()
                .id(loanInstallment.getIdLoanInstallment())
                .idLoan(loanInstallment.getIdLoan())
                .installmentNumber(loanInstallment.getInstallmentNumber())
                .installmentAmountToPay(MoneyUtils.getBigDecimal(loanInstallment.getInstallmentAmountToPay().getNumber()))
                .installmentAmountPaid(MoneyUtils.getBigDecimal(loanInstallment.getInstallmentAmountPaid().getNumber()))
                .paymentDeadline(loanInstallment.getPaymentDeadline())
                .paymentDate(loanInstallment.getPaymentDate())
                .paymentStatus(loanInstallment.getPaymentStatus())
                .build();
    }

    public LoanInstallment toDomain(LoanInstallmentDbDto dto) {
        return LoanInstallment.builder()
                .idLoanInstallment(dto.getId())
                .idLoan(dto.getIdLoan())
                .installmentNumber(dto.getInstallmentNumber())
                .installmentAmountToPay(Money.of(dto.getInstallmentAmountToPay(),"PLN"))
                .installmentAmountPaid(Money.of(dto.getInstallmentAmountPaid(),"PLN"))
                .paymentDeadline(dto.getPaymentDeadline())
                .paymentDate(dto.getPaymentDate())
                .paymentStatus(dto.getPaymentStatus())
                .build();
    }
}