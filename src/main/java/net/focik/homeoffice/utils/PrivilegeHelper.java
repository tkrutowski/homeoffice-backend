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

    public static final String GO_INVOICE_DELETE_ALL = "GO_INVOICE_DELETE_ALL";
    public static final String GO_INVOICE_READ_ALL = "GOAHEAD_READ_ALL";
    public static final String GO_INVOICE_WRITE_ALL = "GOAHEAD_WRITE_ALL";

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
