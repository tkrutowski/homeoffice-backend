package net.focik.homeoffice.utils.share;

import lombok.Getter;

@Getter
public enum Vat {
    VAT_0("0", "0", 0, 1),
    VAT_ZW("zw.", "zw", 0, 1),
    VAT_8("8%", "8", 8, 1.08),
    VAT_23("23%", "23", 23, 1.23),
    VAT_5("5%", "5", 5, 1.05);

    private final String viewValue;
    private final String ksefValue;
    private final int numberValue;
    private final double multiplier;

    Vat(String viewValue, String ksefValue, int numberValue, double multiplier) {
        this.viewValue = viewValue;
        this.ksefValue = ksefValue;
        this.numberValue = numberValue;
        this.multiplier = multiplier;
    }

}
