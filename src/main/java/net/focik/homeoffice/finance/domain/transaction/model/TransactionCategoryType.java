package net.focik.homeoffice.finance.domain.transaction.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
public enum TransactionCategoryType {
    INCOME("Przychód"),
    EXPENSE("Wydatek");

    private final String translate;

    TransactionCategoryType(String translate) {
        this.translate = translate;
    }

    @JsonCreator
    public static TransactionCategoryType fromString(String value) {
        return TransactionCategoryType.valueOf(value.toUpperCase());
    }
}
