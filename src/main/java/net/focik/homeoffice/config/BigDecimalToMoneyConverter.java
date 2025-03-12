package net.focik.homeoffice.config;

import org.javamoney.moneta.Money;
import org.modelmapper.AbstractConverter;

import java.math.BigDecimal;

public class BigDecimalToMoneyConverter extends AbstractConverter<BigDecimal, Money> {
    @Override
    protected Money convert(BigDecimal source) {
        System.out.println();
        try {

        return Money.of(source.doubleValue(), "PLN");
        }
        catch (NumberFormatException e) {
            System.out.println();
        }
        return null;
    }
}
