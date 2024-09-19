package net.focik.homeoffice.goahead.domain.customer;

public enum CustomerType {
    CUSTOMER("Klient"),
    COMPANY("Firma");

    private final String viewValue;

    CustomerType(String viewValue) {
        this.viewValue = viewValue;
    }

    public String getViewValue() {
        return viewValue;
    }
}
