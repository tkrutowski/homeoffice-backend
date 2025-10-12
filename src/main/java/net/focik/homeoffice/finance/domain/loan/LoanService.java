package net.focik.homeoffice.finance.domain.loan;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.finance.domain.exception.LoanInstallmentNotFoundException;
import net.focik.homeoffice.finance.domain.exception.LoanNotFoundException;
import net.focik.homeoffice.finance.domain.exception.LoanNotValidException;
import net.focik.homeoffice.finance.domain.loan.port.secondary.LoanRepository;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.javamoney.moneta.Money;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
class LoanService {

    private final LoanRepository loanRepository;

    //-----------------------------LOAN---------------------------


    @Transactional
    public Loan saveLoan(Loan loan) {
        if (isNotValid(loan))
            throw new LoanNotValidException();
        Loan saved = loanRepository.saveLoan(loan);
        List<LoanInstallment> loanInstallments = new ArrayList<>();
        for (int i = 0; i < saved.getNumberOfInstallments(); i++) {
            loanInstallments.add(LoanInstallment.builder()
                    .idLoan(saved.getId())
                    .installmentNumber(i + 1)
                    .installmentAmountToPay(loan.getInstallmentAmount())
                    .installmentAmountPaid(Money.of(0, "PLN"))
                    .paymentDeadline(loan.getFirstPaymentDate().plusMonths(i))
                    .paymentStatus(PaymentStatus.TO_PAY)
                    .build());
        }
        List<LoanInstallment> savedLoanInstallments = addLoanInstallment(loanInstallments);
        saved.setInstallments(savedLoanInstallments);
        return saved;
    }

    public LoanInstallment addLoanInstallment(LoanInstallment loanInstallment) {
        return loanRepository.saveLoanInstallment(loanInstallment);
    }

    public List<LoanInstallment> addLoanInstallment(List<LoanInstallment> loanInstallment) {
        return loanRepository.saveLoanInstallment(loanInstallment);
    }

    Money getInstallmentLoansSumByStatus(int idLoan, PaymentStatus status) {
        Money sum = Money.of(0, "PLN");
        List<LoanInstallment> installments = loanRepository.findLoanInstallmentByLoanId(idLoan);

        if (status != null && status != PaymentStatus.ALL) {
            installments = installments.stream()
                    .filter(loanInstallment -> status.equals(loanInstallment.getPaymentStatus()))
                    .toList();
        }

        if (installments != null && !installments.isEmpty()) {
            for (LoanInstallment installment : installments) {
                sum = sum.add(installment.getInstallmentAmountToPay());
            }
        }
        return sum;
    }

    List<Loan> findLoansByUser(int idUser, PaymentStatus loanStatus, boolean withLoanInstallment) {
        List<Loan> loanByUserId = loanRepository.findLoanByUserId(idUser);

        if (withLoanInstallment) {
            for (Loan l : loanByUserId) {
                List<LoanInstallment> loanInstallmentList = findLoanInstallmentByLoanId(l.getId());
                l.addLoanInstallment(loanInstallmentList);
            }
        }

        if (loanStatus == null || loanStatus.equals(PaymentStatus.ALL))
            return loanByUserId;

        loanByUserId = loanByUserId.stream()
                .filter(loan -> loan.getLoanStatus().equals(loanStatus))
                .collect(Collectors.toList());


        return loanByUserId;
    }

    List<LoanInstallment> findLoanInstallmentByLoanId(int idLoan) {
        return loanRepository.findLoanInstallmentByLoanId(idLoan);
    }

    Loan findLoanById(int idLoan, boolean withLoanInstallment) {
        Optional<Loan> loanById = loanRepository.findLoanById(idLoan);

        if (loanById.isEmpty()) {
            throw new LoanNotFoundException(idLoan);
        }

        if (withLoanInstallment) {
            List<LoanInstallment> loanInstallmentList = findLoanInstallmentByLoanId(loanById.get().getId());
            loanById.get().addLoanInstallment(loanInstallmentList);
        }

        return loanById.get();
    }

    List<Loan> findLoansByStatus(PaymentStatus loanStatus, boolean withInstallment) {
        List<Loan> loans = loanRepository.findAll();

        if (withInstallment) {
            for (Loan l : loans) {
                List<LoanInstallment> loanInstallmentList = findLoanInstallmentByLoanId(l.getId());
                l.addLoanInstallment(loanInstallmentList);
            }
        }

        if (loanStatus == null || PaymentStatus.ALL.equals(loanStatus))
            return loans;

        loans = loans.stream()
                .filter(loan -> loan.getLoanStatus().equals(loanStatus))
                .collect(Collectors.toList());

        return loans;
    }

    @Transactional
    public void deleteLoan(int idLoan) {
        loanRepository.deleteLoanInstallmentByIdLoan(idLoan);
        loanRepository.deleteLoanById(idLoan);
    }

    public void updateLoan(Loan loan) {
        if (isNotValid(loan))
            throw new LoanNotValidException();
        loanRepository.saveLoan(loan);
    }

    public LoanInstallment updateLoanInstallment(LoanInstallment loanInstallment) {
//        if (isNotValid(loanInstallment))
//            throw new LoanNotValidException();
        return loanRepository.saveLoanInstallment(loanInstallment);
    }

    public void deleteLoanInstallment(int id) {
        loanRepository.deleteLoanInstallmentById(id);
    }

    private boolean isNotValid(Loan a) {
        if (Objects.equals(a.getAmount().getNumberStripped(), BigDecimal.ZERO))
            return true;
        if (Objects.equals(a.getInstallmentAmount().getNumberStripped(), BigDecimal.ZERO))
            return true;
        return a.getDate() == null;
    }

    private boolean isNotValid(LoanInstallment loanInstallment) {
        if (Objects.equals(loanInstallment.getInstallmentAmountPaid().getNumberStripped(), BigDecimal.ZERO))
            return true;
        return loanInstallment.getPaymentDate() == null;
    }


    public LoanInstallment getLoanInstallment(int idLoanInstallment) {
        Optional<LoanInstallment> loanInstallmentById = loanRepository.findLoanInstallmentById(idLoanInstallment);

        if (loanInstallmentById.isEmpty()) {
            throw new LoanInstallmentNotFoundException(idLoanInstallment);
        }
        return loanInstallmentById.get();
    }

    public List<Loan> getLoansByBank(Integer idBank) {
        return loanRepository.findLoanByBankId(idBank);
    }

    public Page<Loan> findLoansPageableWithFilters(int page, int size, String sortField, String sortDirection, String globalFilter, String name, Integer idBank, LocalDate date, String dateComparisonType, BigDecimal amount, String amountComparisonType, PaymentStatus status) {
        String jpaField;
        if ("bankName".equals(sortField)) {
            jpaField = "bank.name";
        } else {
            jpaField = sortField.isEmpty() || "null".equals(sortField) ? "id" : sortField;
        }

        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, jpaField));

        Page<Loan> loanWithFilters = loanRepository.findLoanWithFilters(globalFilter, name, idBank, date, dateComparisonType, amount, amountComparisonType, status, pageable);
        loanWithFilters.forEach(loan -> {
            List<LoanInstallment> loanInstallmentList = findLoanInstallmentByLoanId(loan.getId());
            loan.addLoanInstallment(loanInstallmentList);
        });

        return loanWithFilters;
    }
}
