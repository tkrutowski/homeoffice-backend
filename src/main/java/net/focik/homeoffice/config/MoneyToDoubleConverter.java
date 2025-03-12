package net.focik.homeoffice.config;

import org.javamoney.moneta.Money;
import org.modelmapper.AbstractConverter;

import java.util.Objects;

public class MoneyToDoubleConverter extends AbstractConverter<Money, Double> {
    @Override
    protected Double convert(Money source) {
        System.out.println();
        if (Objects.isNull(source))
            return 0.0;
        return source.getNumberStripped().doubleValue();
    }
}
