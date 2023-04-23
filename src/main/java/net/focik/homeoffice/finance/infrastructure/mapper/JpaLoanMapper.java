package net.focik.homeoffice.finance.infrastructure.mapper;

import net.focik.homeoffice.finance.domain.loan.Loan;
import net.focik.homeoffice.finance.domain.loan.LoanInstallment;
import net.focik.homeoffice.finance.infrastructure.dto.LoanDbDto;
import net.focik.homeoffice.finance.infrastructure.dto.LoanInstallmentDbDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class JpaLoanMapper {

    public LoanDbDto toDto(Loan loan) {
        return LoanDbDto.builder()
                .id(loan.getId())
                .idBank(loan.getIdBank())
                .idUser(loan.getIdUser())
                .amount(loan.getAmount())
                .date(loan.getDate())
                .loanNumber(loan.getLoanNumber())
                .accountNumber(loan.getAccountNumber())
                .firstPaymentDate(loan.getFirstPaymentDate())
                .numberOfInstallments(loan.getNumberOfInstallments())
                .installmentAmount(loan.getInstallmentAmount())
                .name(loan.getName())
                .otherInfo(loan.getOtherInfo())
                .loanStatus(loan.getLoanStatus())
                .loanCost(loan.getLoanCost())
                .build();
    }

    public Loan toDomain(LoanDbDto dto, List<LoanInstallmentDbDto> loanInstallments) {
        return Loan.builder()
                .id(dto.getId())
                .idBank(dto.getIdBank())
                .idUser(dto.getIdUser())
                .name(dto.getName())
                .amount(dto.getAmount())
                .date(dto.getDate())
                .loanNumber(dto.getLoanNumber())
                .accountNumber(dto.getAccountNumber())
                .firstPaymentDate(dto.getFirstPaymentDate())
                .numberOfInstallments(dto.getNumberOfInstallments())
                .installmentAmount(dto.getInstallmentAmount())
                .loanStatus(dto.getLoanStatus())
                .loanCost(dto.getLoanCost())
                .otherInfo(dto.getOtherInfo())
                .loanInstallments(mapListLoanInstallmentToSet(loanInstallments))
                .build();
    }

    public Loan toDomain(LoanDbDto dto) {
        return Loan.builder()
                .id(dto.getId())
                .idBank(dto.getIdBank())
                .idUser(dto.getIdUser())
                .name(dto.getName())
                .amount(dto.getAmount())
                .date(dto.getDate())
                .loanNumber(dto.getLoanNumber())
                .accountNumber(dto.getAccountNumber())
                .firstPaymentDate(dto.getFirstPaymentDate())
                .numberOfInstallments(dto.getNumberOfInstallments())
                .installmentAmount(dto.getInstallmentAmount())
                .loanStatus(dto.getLoanStatus())
                .loanCost(dto.getLoanCost())
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
                .installmentAmountToPay(loanInstallment.getInstallmentAmountToPay())
                .installmentAmountPaid(loanInstallment.getInstallmentAmountPaid())
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
                .installmentAmountToPay(dto.getInstallmentAmountToPay())
                .installmentAmountPaid(dto.getInstallmentAmountPaid())
                .paymentDeadline(dto.getPaymentDeadline())
                .paymentDate(dto.getPaymentDate())
                .paymentStatus(dto.getPaymentStatus())
                .build();
    }
}
