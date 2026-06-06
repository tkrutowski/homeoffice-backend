package net.focik.homeoffice.finance.domain.transaction.model;

import lombok.Getter;

@Getter
public enum TransactionType {
    TRANSFER_OUT("Przelew wychodzący"),
    TRANSFER_IN("Przelew przychodzący"),
    WITHDRAWAL("Wypłata"),
    DEPOSIT("Wpłata"),
    CARD_PAYMENT("Spłata karty kredytowj");
    private final String translate;

    TransactionType(String translate) {
        this.translate = translate;
    }
}
