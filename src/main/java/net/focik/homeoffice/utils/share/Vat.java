package net.focik.homeoffice.utils.share;

import lombok.Getter;

@Getter
public enum Vat {
    VAT_0("0", 0, 1),
    VAT_8("8%", 8, 1.08),
    VAT_23("23%", 23, 1.23);

    private final String viewValue;
    private final int numberValue;
    private final double multiplier;

    Vat(String viewValue, int numberValue, double multiplier) {
        this.viewValue = viewValue;
        this.numberValue = numberValue;
        this.multiplier = multiplier;
    }

}
