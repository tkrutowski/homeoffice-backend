package net.focik.homeoffice.config;

import org.javamoney.moneta.Money;
import org.modelmapper.AbstractConverter;

public class DoubleToMoneyConverter extends AbstractConverter<Double, Money> {
    @Override
    protected Money convert(Double source) {
        System.out.println();
        try {

        return Money.of(source, "PLN");
        }
        catch (NumberFormatException e) {
            System.out.println();
        }
        return null;
    }
}