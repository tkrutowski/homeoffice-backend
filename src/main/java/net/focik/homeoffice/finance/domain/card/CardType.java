package net.focik.homeoffice.finance.domain.card;

/**
 * Sposób rozliczania płatności kartą.
 */
public enum CardType {

    /**
     * Karta kredytowa z cyklem rozliczeniowym - zakupy z danego okresu (do {@link Card#getClosingDay()})
     * są rozliczane jedną wspólną płatnością w dniu {@link Card#getRepaymentDay()}.
     */
    CREDIT,

    /**
     * Płatność odroczona (np. PayPo, Allegro) - każdy zakup jest rozliczany osobno,
     * a termin płatności liczony jest od daty zakupu ({@link Card#getPaymentTermDays()} dni).
     */
    DEFERRED_PAYMENT
}
