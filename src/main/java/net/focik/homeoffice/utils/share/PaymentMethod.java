package net.focik.homeoffice.utils.share;

import lombok.Getter;

@Getter
public enum PaymentMethod {
    CASH(1, "Gotówka"),
    CASH_LATE(1, "Płatność odroczona"),
    TRANSFER(6, "Przelew");

    private final int ksefCode;
    private final String translate;

    PaymentMethod(int ksefCode, String translate) {
        this.ksefCode = ksefCode;
        this.translate = translate;
    }
}
