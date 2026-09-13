package net.focik.homeoffice.utils.share;

public enum PaymentStatus {
    PAID, TO_PAY, OVER_DUE, ALL,
    /** Purchase wchłonięty przez Loan (zob. Purchase.idLoan) - płatność idzie już przez raty kredytu. */
    CONVERTED
}
