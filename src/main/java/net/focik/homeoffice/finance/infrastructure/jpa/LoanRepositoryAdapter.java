package net.focik.homeoffice.finance.infrastructure.jpa;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.finance.domain.loan.Loan;
import net.focik.homeoffice.finance.domain.loan.LoanInstallment;
import net.focik.homeoffice.finance.domain.loan.port.secondary.LoanRepository;
import net.focik.homeoffice.finance.infrastructure.dto.LoanDbDto;
import net.focik.homeoffice.finance.infrastructure.dto.LoanInstallmentDbDto;
import net.focik.homeoffice.finance.infrastructure.mapper.JpaLoanMapper;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Primary
@AllArgsConstructor
class LoanRepositoryAdapter implements LoanRepository {

    LoanDtoRepository loanDtoRepository;
    LoanInstallmentDtoRepository loanInstallmentDtoRepository;
    JpaLoanMapper mapper;


    @Override
    public Loan saveLoan(Loan loan) {
        LoanDbDto saved = loanDtoRepository.save(mapper.toDto(loan));
        return mapper.toDomain(saved);
    }

    @Override
    public LoanInstallment saveLoanInstallment(LoanInstallment loanInstallment) {
        LoanInstallmentDbDto saved = loanInstallmentDtoRepository.save(mapper.toDto(loanInstallment));
        return mapper.toDomain(saved);
    }

    @Override
    public List<LoanInstallment> saveLoanInstallment(List<LoanInstallment> loanInstallments) {
        List<LoanInstallmentDbDto> dbDtoList = loanInstallments.stream().map(mapper::toDto).collect(Collectors.toList());
        List<LoanInstallmentDbDto> loanInstallmentDbDtos = loanInstallmentDtoRepository.saveAll(dbDtoList);
        return loanInstallmentDbDtos.stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<Loan> findLoanById(Integer id) {
        Optional<LoanDbDto> loanById = loanDtoRepository.findById(id);
        return loanById.map(loanDbDto -> mapper.toDomain(loanDbDto));
    }

    @Override
    public Optional<LoanInstallment> findLoanInstallmentById(Integer id) {
        Optional<LoanInstallmentDbDto> byId = loanInstallmentDtoRepository.findById(id);
        return byId.map(loanInstallmentDbDto -> mapper.toDomain(loanInstallmentDbDto));
    }

    @Override
    public List<Loan> findLoanByUserId(Integer idUser) {
        return loanDtoRepository.findAllByIdUser(idUser).stream()
                .map(loanDto -> mapper.toDomain(loanDto))
                .collect(Collectors.toList());
    }

    @Override
    public List<Loan> findLoanByBankId(Integer idBank) {
        return loanDtoRepository.findAllByBank_Id(idBank).stream()
                .map(loanDto -> mapper.toDomain(loanDto))
                .collect(Collectors.toList());
    }

    @Override
    public List<Loan> findAll() {
        return loanDtoRepository.findAll().stream()
                .map(loanDto -> mapper.toDomain(loanDto))
                .collect(Collectors.toList());
    }

    @Override
    public List<LoanInstallment> findLoanInstallmentByLoanId(Integer loanId) {
        return loanInstallmentDtoRepository.findAllByIdLoan(loanId).stream()
                .map(loanInstallmentDto -> mapper.toDomain(loanInstallmentDto))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteLoanById(int idLoan) {
        loanDtoRepository.deleteById(idLoan);
    }

    @Override
    public void deleteLoanInstallmentById(int idLoanInstallment) {
        loanInstallmentDtoRepository.deleteById(idLoanInstallment);
    }

    @Override
    public void deleteLoanInstallmentByIdLoan(int idLoan) {
        loanInstallmentDtoRepository.deleteByIdLoan(idLoan);
    }
}
