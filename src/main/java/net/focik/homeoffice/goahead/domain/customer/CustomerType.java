package net.focik.homeoffice.goahead.domain.customer;

import lombok.Getter;

@Getter
public enum CustomerType {
    CUSTOMER("Klient"),
    COMPANY("Firma"),
    SELLER("Sprzedawca");

    private final String viewValue;

    CustomerType(String viewValue) {
        this.viewValue = viewValue;
    }

}
