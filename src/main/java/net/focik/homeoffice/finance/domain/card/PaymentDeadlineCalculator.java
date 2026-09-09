package net.focik.homeoffice.finance.domain.card;

import net.focik.homeoffice.finance.domain.exception.CardNotValidException;

import java.time.LocalDate;

/**
 * Wylicza termin płatności zakupu w zależności od typu karty:
 * <ul>
 *     <li>{@link CardType#CREDIT} - zakup wpada do cyklu rozliczeniowego zamykanego {@code closingDay}
 *     danego miesiąca (jeśli zakup wypadł po tym dniu, trafia do cyklu kolejnego miesiąca),
 *     a płatność za cały cykl następuje w {@code repaymentDay} miesiąca następującego po jego zamknięciu,</li>
 *     <li>{@link CardType#DEFERRED_PAYMENT} - termin liczony indywidualnie dla zakupu,
 *     jako data zakupu powiększona o {@code paymentTermDays} dni.</li>
 * </ul>
 */
public final class PaymentDeadlineCalculator {

    private PaymentDeadlineCalculator() {
    }

    public static LocalDate calculate(Card card, LocalDate purchaseDate) {
        if (card == null || purchaseDate == null) {
            throw new CardNotValidException("Card and purchase date are required to calculate payment deadline.");
        }
        if (card.getCardType() == null) {
            throw new CardNotValidException("Card type is required to calculate payment deadline.");
        }

        return switch (card.getCardType()) {
            case CREDIT -> calculateCreditDeadline(purchaseDate, card.getClosingDay(), card.getRepaymentDay());
            case DEFERRED_PAYMENT -> calculateDeferredPaymentDeadline(purchaseDate, card.getPaymentTermDays());
        };
    }

    private static LocalDate calculateCreditDeadline(LocalDate purchaseDate, Integer closingDay, Integer repaymentDay) {
        if (closingDay == null || repaymentDay == null) {
            throw new CardNotValidException("Credit card requires closingDay and repaymentDay to calculate payment deadline.");
        }

        int monthsToAdd = purchaseDate.getDayOfMonth() > closingDay ? 2 : 1;
        LocalDate deadline = purchaseDate.plusMonths(monthsToAdd);
        int day = Math.min(repaymentDay, deadline.lengthOfMonth());
        return deadline.withDayOfMonth(day);
    }

    private static LocalDate calculateDeferredPaymentDeadline(LocalDate purchaseDate, Integer paymentTermDays) {
        if (paymentTermDays == null) {
            throw new CardNotValidException("Deferred payment card requires paymentTermDays to calculate payment deadline.");
        }

        return purchaseDate.plusDays(paymentTermDays);
    }
}
