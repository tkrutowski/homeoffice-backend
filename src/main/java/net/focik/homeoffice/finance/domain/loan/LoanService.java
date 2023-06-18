package net.focik.homeoffice.finance.domain.loan;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.finance.domain.exception.LoanInstallmentNotFoundException;
import net.focik.homeoffice.finance.domain.exception.LoanNotFoundException;
import net.focik.homeoffice.finance.domain.exception.LoanNotValidException;
import net.focik.homeoffice.finance.domain.loan.port.secondary.LoanRepository;
import net.focik.homeoffice.utils.MoneyUtils;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.javamoney.moneta.Money;
import org.springframework.stereotype.Service;

import javax.money.CurrencyUnit;
import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
class LoanService {

    private final LoanRepository loanRepository;

    //-----------------------------LOAN---------------------------


    @Transactional
    Loan saveLoan(Loan loan) {
        if (isNotValid(loan))
            throw new LoanNotValidException();
        Loan saved = loanRepository.saveLoan(loan);
        List<LoanInstallment> loanInstallments = new ArrayList<>();
        for (int i = 0; i < saved.getNumberOfInstallments(); i++) {
            loanInstallments.add(LoanInstallment.builder()
                    .idLoan(saved.getId())
                    .installmentNumber(i + 1)
                    .installmentAmountToPay(loan.getInstallmentAmount())
                    .installmentAmountPaid(BigDecimal.ZERO)
                    .paymentDeadline(loan.getFirstPaymentDate().plusMonths(i))
                    .paymentStatus(PaymentStatus.TO_PAY)
                    .build());
        }
        List<LoanInstallment> savedLoanInstallments = addLoanInstallment(loanInstallments);
        saved.setLoanInstallments(savedLoanInstallments);
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

        if(status != null && status != PaymentStatus.ALL){
            installments = installments.stream()
                .filter(loanInstallment -> status.equals(loanInstallment.getPaymentStatus()))
                    .collect(Collectors.toList());
        }

        if (installments != null && !installments.isEmpty()) {
            for (LoanInstallment installment : installments) {
                sum = sum.add(Money.of(installment.getInstallmentAmountToPay(), "PLN"));
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

        if (loanStatus == null)
            return loanByUserId;

        loanByUserId = loanByUserId.stream()
                .filter(loan -> loan.getLoanStatus().equals(loanStatus))
                .collect(Collectors.toList());


        return loanByUserId;
    }

    List<LoanInstallment> findLoanInstallmentByLoanId(int idLoan) {
        return loanRepository.findLoanInstallmentByLoanId(idLoan);
    }

    List<LoanInstallment> getLoanInstallments(Integer idEmployee, LocalDate date) {
        List<Loan> loansByEmployee = findLoansByUser(idEmployee, null, true);
        return loansByEmployee.stream()
                .map(Loan::getLoanInstallments)
                .flatMap(Collection::stream)
                .filter(loanInstallment -> loanInstallment.getPaymentDeadline().getYear() == (date.getYear()))
                .filter(loanInstallment -> loanInstallment.getPaymentDeadline().getMonth().equals(date.getMonth()))
                .collect(Collectors.toList());
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

    public Loan updateLoan(Loan loan) {
        if (isNotValid(loan))
            throw new LoanNotValidException();
        return loanRepository.saveLoan(loan);
    }

    public LoanInstallment updateLoanInstallment(LoanInstallment loanInstallment) {
        if (isNotValid(loanInstallment))
            throw new LoanNotValidException();
        return loanRepository.saveLoanInstallment(loanInstallment);
    }

    public void deleteLoanInstallment(int id) {
        loanRepository.deleteLoanInstallmentById(id);
    }

    private boolean isNotValid(Loan a) {
        if (Objects.equals(a.getAmount(), BigDecimal.ZERO))
            return true;
        if (Objects.equals(a.getInstallmentAmount(), BigDecimal.ZERO))
            return true;
        return a.getDate() == null;
    }

    private boolean isNotValid(LoanInstallment loanInstallment) {
        if (Objects.equals(loanInstallment.getInstallmentAmountPaid(), BigDecimal.ZERO))
            return true;
        return loanInstallment.getPaymentDate() == null;
    }

    public Money getLoansToPaySum(Integer idUser) {
        List<Loan> loans = findLoansByUser(idUser, PaymentStatus.TO_PAY, true);
        Money result = Money.of(BigDecimal.ZERO, "PLN");

        for (Loan loan : loans) {
            Money loanAmount = MoneyUtils.mapToMoney(loan.getAmount());
            BigDecimal reduce = loan.getLoanInstallments().stream()
                    .map(LoanInstallment::getInstallmentAmountToPay)
                    .reduce(BigDecimal::add)
                    .orElse(BigDecimal.ZERO);
            Money loanInstallmentPayedOff = Money.of(reduce, "PLN");
            result = result.add(loanAmount.subtract(loanInstallmentPayedOff));
        }

        return result;
    }

    public LoanInstallment getLoanInstallment(int idLoanInstallment) {
        Optional<LoanInstallment> loanInstallmentById = loanRepository.findLoanInstallmentById(idLoanInstallment);

        if (loanInstallmentById.isEmpty()){
            throw new LoanInstallmentNotFoundException(idLoanInstallment);
        }
        return loanInstallmentById.get();
    }
}
