package net.focik.homeoffice.finance.domain.exception;

import net.focik.homeoffice.utils.exceptions.ObjectNotValidException;

import java.math.BigDecimal;

public class LoanAmountMismatchException extends ObjectNotValidException {
    public LoanAmountMismatchException(BigDecimal purchasesSum, BigDecimal loanAmount) {
        super("Sum of selected purchases (" + purchasesSum + ") does not match loan amount (" + loanAmount + ")");
    }
}
