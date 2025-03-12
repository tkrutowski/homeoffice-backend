package net.focik.homeoffice.config;

import org.javamoney.moneta.Money;
import org.modelmapper.AbstractConverter;

import java.math.BigDecimal;
import java.util.Objects;

public class MoneyToBigDecimalConverter extends AbstractConverter<Money, BigDecimal> {
    @Override
    protected BigDecimal convert(Money source) {
        System.out.println();
        if (Objects.isNull(source))
            return BigDecimal.ZERO;
        return source.getNumberStripped();
    }
}
