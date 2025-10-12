package net.focik.homeoffice.finance.infrastructure.jpa;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.finance.domain.loan.Loan;
import net.focik.homeoffice.finance.domain.loan.LoanInstallment;
import net.focik.homeoffice.finance.domain.loan.port.secondary.LoanRepository;
import net.focik.homeoffice.finance.infrastructure.dto.LoanDbDto;
import net.focik.homeoffice.finance.infrastructure.dto.LoanInstallmentDbDto;
import net.focik.homeoffice.finance.infrastructure.mapper.JpaLoanMapper;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@Primary
@AllArgsConstructor
class LoanRepositoryAdapter implements LoanRepository {

    LoanDtoRepository loanDtoRepository;
    LoanInstallmentDtoRepository loanInstallmentDtoRepository;
    JpaLoanMapper mapper;


    @Override
    public Loan saveLoan(Loan loan) {
        LoanDbDto dto = mapper.toDto(loan);
        log.debug("Mapping loan to DTO: {}", dto);
        LoanDbDto saved = loanDtoRepository.save(dto);
        return mapper.toDomain(saved);
    }

    @Override
    public LoanInstallment saveLoanInstallment(LoanInstallment loanInstallment) {
        LoanInstallmentDbDto saved = loanInstallmentDtoRepository.save(mapper.toDto(loanInstallment));
        return mapper.toDomain(saved);
    }

    @Override
    public List<LoanInstallment> saveLoanInstallment(List<LoanInstallment> loanInstallments) {
        List<LoanInstallmentDbDto> dbDtoList = loanInstallments.stream()
                .map(installment -> {
                    LoanInstallmentDbDto dto = mapper.toDto(installment);
                    if (dto.getId() == 0) {
                        dto.setId(null);
                    }
                    return dto;
                })
                .collect(Collectors.toList());
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

    @Override
    public Page<Loan> findLoanWithFilters(String globalFilter, String name, Integer idBank, LocalDate date, String dateComparisonType, BigDecimal amount, String amountComparisonType, PaymentStatus status, Integer idUser, Pageable pageable) {
        Page<LoanDbDto> loanPage = loanDtoRepository.findLoanWithFilters(globalFilter, name, idBank, date, dateComparisonType, amount, amountComparisonType, status, idUser, pageable);

        return loanPage.map(mapper::toDomain);
    }
}
