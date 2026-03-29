package net.focik.homeoffice.utils.share;

import lombok.Getter;

@Getter
public enum PaymentMethod {
    CASH(1),
    CASH_LATE(1),
    TRANSFER(6);

    private final int ksefCode;

    PaymentMethod(int ksefCode) {
        this.ksefCode = ksefCode;
    }
}
