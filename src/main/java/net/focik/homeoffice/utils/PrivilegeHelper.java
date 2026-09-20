package net.focik.homeoffice.utils;

import java.util.List;

public class PrivilegeHelper {
    public static final String AUTHORITIES = "authorities";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String GO_CUSTOMER_READ_ALL = "HR_EMPLOYEE_READ_ALL";
    public static final String GO_CUSTOMER_READ = "HR_EMPLOYEE_READ";
    public static final String GO_CUSTOMER_WRITE_ALL = "HR_EMPLOYEE_WRITE_ALL";
    public static final String GO_CUSTOMER_DELETE_ALL = "GO_CUSTOMER_DELETE_ALL";



    public static final String FINANCE_FEE_READ_ALL = "FINANCE_FEE_READ_ALL";
    public static final String FINANCE_FEE_READ = "FINANCE_FEE_READ";
    public static final String FINANCE_LOAN_READ_ALL = "FINANCE_LOAN_READ_ALL";
    public static final String FINANCE_LOAN_READ = "FINANCE_LOAN_READ";
    public static final String FINANCE_PAYMENT_READ_ALL = "FINANCE_PAYMENT_READ_ALL";
    public static final String FINANCE_PAYMENT_READ = "FINANCE_PAYMENT_READ";
    public static final String FINANCE_LOAN_WRITE_ALL = "FINANCE_LOAN_WRITE_ALL";
    public static final String FINANCE_LOAN_WRITE = "FINANCE_LOAN_WRITE";
    public static final String FINANCE_PAYMENT_WRITE_ALL = "FINANCE_PAYMENT_WRITE_ALL";
    public static final String FINANCE_PAYMENT_WRITE = "FINANCE_PAYMENT_WRITE";
    public static final String FINANCE_LOAN_DELETE_ALL = "FINANCE_LOAN_DELETE_ALL";
    public static final String FINANCE_LOAN_DELETE = "FINANCE_LOAN_DELETE";
    public static final String FINANCE_PAYMENT_DELETE_ALL = "FINANCE_PAYMENT_DELETE_ALL";
    public static final String FINANCE_PAYMENT_DELETE = "FINANCE_PAYMENT_DELETE";
    public static final String FINANCE_FEE_WRITE_ALL = "FINANCE_FEE_WRITE_ALL";
    public static final String FINANCE_FEE_WRITE = "FINANCE_FEE_WRITE";
    public static final String FINANCE_FEE_DELETE_ALL = "FINANCE_FEE_DELETE_ALL";
    public static final String FINANCE_FEE_DELETE = "FINANCE_FEE_DELETE";
    public static final String FINANCE_PURCHASE_READ_ALL = "FINANCE_PURCHASE_READ_ALL";
    public static final String FINANCE_PURCHASE_READ = "FINANCE_PURCHASE_READ";
    public static final String FINANCE_PURCHASE_WRITE_ALL = "FINANCE_PURCHASE_WRITE_ALL";
    public static final String FINANCE_PURCHASE_WRITE = "FINANCE_PURCHASE_WRITE";
    public static final String FINANCE_PURCHASE_DELETE_ALL = "FINANCE_PURCHASE_DELETE_ALL";
    public static final String FINANCE_PURCHASE_DELETE = "FINANCE_PURCHASE_DELETE";

    // Uprawnienia generyczne (nie per-modul) - juz uzywane jako literaly w CardController,
    // BankController, FirmController; te stale tylko je porzadkuja. Dotycza zasobow "referencyjnych"
    // (karty, banki, firmy), gdzie READ/WRITE/DELETE (bez _ALL) oznacza "tylko wlasne".
    public static final String FINANCE_READ_ALL = "FINANCE_READ_ALL";
    public static final String FINANCE_READ = "FINANCE_READ";
    public static final String FINANCE_WRITE_ALL = "FINANCE_WRITE_ALL";
    public static final String FINANCE_WRITE = "FINANCE_WRITE";
    public static final String FINANCE_DELETE_ALL = "FINANCE_DELETE_ALL";
    public static final String FINANCE_DELETE = "FINANCE_DELETE";

    public static final String GO_INVOICE_DELETE_ALL = "GO_INVOICE_DELETE_ALL";
    public static final String GO_INVOICE_READ_ALL = "GOAHEAD_READ";
    public static final String GO_INVOICE_WRITE_ALL = "GOAHEAD_WRITE";

    private PrivilegeHelper() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * @param roles       list of access roles
     * @param searchRoles list of roles to be checked
     * @return true if any role from @roles is found in @searchRoles
     */
    public static boolean dontHaveAccess(List<String> roles, List<String> searchRoles) {
        for (String role : roles) {
            if (searchRoles.contains(role))
                return false;
        }
        return true;
    }
}
