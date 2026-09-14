package net.focik.homeoffice.finance.domain.card;

import net.focik.homeoffice.finance.domain.exception.CardNotValidException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentDeadlineCalculatorTest {

    // ---------- CREDIT ----------

    @Test
    @DisplayName("credit card: should fall into the next month's cycle when purchased before the closing day")
    void calculate_Credit_ShouldFallIntoNextMonthCycle_WhenPurchasedBeforeClosingDay() {
        Card card = creditCard(20, 10);
        LocalDate purchaseDate = LocalDate.of(2026, 3, 15); // 15 <= closingDay(20)

        LocalDate deadline = PaymentDeadlineCalculator.calculate(card, purchaseDate);

        assertThat(deadline).isEqualTo(LocalDate.of(2026, 4, 10));
    }

    @Test
    @DisplayName("credit card: should fall into the cycle after next when purchased after the closing day")
    void calculate_Credit_ShouldFallIntoCycleAfterNext_WhenPurchasedAfterClosingDay() {
        Card card = creditCard(20, 10);
        LocalDate purchaseDate = LocalDate.of(2026, 3, 25); // 25 > closingDay(20)

        LocalDate deadline = PaymentDeadlineCalculator.calculate(card, purchaseDate);

        assertThat(deadline).isEqualTo(LocalDate.of(2026, 5, 10));
    }

    @Test
    @DisplayName("credit card: should not push to the next cycle when purchased exactly on the closing day")
    void calculate_Credit_ShouldNotPushToNextCycle_WhenPurchasedExactlyOnClosingDay() {
        Card card = creditCard(20, 10);
        LocalDate purchaseDate = LocalDate.of(2026, 3, 20); // == closingDay, still current cycle

        LocalDate deadline = PaymentDeadlineCalculator.calculate(card, purchaseDate);

        assertThat(deadline).isEqualTo(LocalDate.of(2026, 4, 10));
    }

    @Test
    @DisplayName("credit card: should roll over into the next year when the cycle crosses New Year")
    void calculate_Credit_ShouldRollOverToNextYear_WhenCycleCrossesNewYear() {
        Card card = creditCard(20, 10);
        LocalDate purchaseDate = LocalDate.of(2026, 12, 25); // after closing day -> +2 months

        LocalDate deadline = PaymentDeadlineCalculator.calculate(card, purchaseDate);

        assertThat(deadline).isEqualTo(LocalDate.of(2027, 2, 10));
    }

    @Test
    @DisplayName("credit card: should clamp the repayment day when the target month is shorter")
    void calculate_Credit_ShouldClampRepaymentDay_WhenTargetMonthIsShorter() {
        Card card = creditCard(20, 31);
        LocalDate purchaseDate = LocalDate.of(2026, 1, 10); // deadline month = February (28 days in 2026)

        LocalDate deadline = PaymentDeadlineCalculator.calculate(card, purchaseDate);

        assertThat(deadline).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    @DisplayName("credit card: should throw when the closing day is missing")
    void calculate_Credit_ShouldThrow_WhenClosingDayMissing() {
        Card card = creditCard(null, 10);

        assertThatThrownBy(() -> PaymentDeadlineCalculator.calculate(card, LocalDate.of(2026, 3, 15)))
                .isInstanceOf(CardNotValidException.class);
    }

    @Test
    @DisplayName("credit card: should throw when the repayment day is missing")
    void calculate_Credit_ShouldThrow_WhenRepaymentDayMissing() {
        Card card = creditCard(20, null);

        assertThatThrownBy(() -> PaymentDeadlineCalculator.calculate(card, LocalDate.of(2026, 3, 15)))
                .isInstanceOf(CardNotValidException.class);
    }

    // ---------- DEFERRED_PAYMENT ----------

    @Test
    @DisplayName("deferred payment card: should add the payment term days to the purchase date")
    void calculate_DeferredPayment_ShouldAddPaymentTermDaysToPurchaseDate() {
        Card card = deferredPaymentCard(30);
        LocalDate purchaseDate = LocalDate.of(2026, 3, 15);

        LocalDate deadline = PaymentDeadlineCalculator.calculate(card, purchaseDate);

        assertThat(deadline).isEqualTo(LocalDate.of(2026, 4, 14));
    }

    @Test
    @DisplayName("deferred payment card: should roll over into the next year when the term crosses New Year")
    void calculate_DeferredPayment_ShouldRollOverToNextYear_WhenTermCrossesNewYear() {
        Card card = deferredPaymentCard(30);
        LocalDate purchaseDate = LocalDate.of(2026, 12, 20);

        LocalDate deadline = PaymentDeadlineCalculator.calculate(card, purchaseDate);

        assertThat(deadline).isEqualTo(LocalDate.of(2027, 1, 19));
    }

    @Test
    @DisplayName("deferred payment card: should throw when the payment term days is missing")
    void calculate_DeferredPayment_ShouldThrow_WhenPaymentTermDaysMissing() {
        Card card = deferredPaymentCard(null);

        assertThatThrownBy(() -> PaymentDeadlineCalculator.calculate(card, LocalDate.of(2026, 3, 15)))
                .isInstanceOf(CardNotValidException.class);
    }

    // ---------- Common guards ----------

    @Test
    @DisplayName("should throw when the card is null")
    void calculate_ShouldThrow_WhenCardIsNull() {
        assertThatThrownBy(() -> PaymentDeadlineCalculator.calculate(null, LocalDate.of(2026, 3, 15)))
                .isInstanceOf(CardNotValidException.class);
    }

    @Test
    @DisplayName("should throw when the purchase date is null")
    void calculate_ShouldThrow_WhenPurchaseDateIsNull() {
        Card card = creditCard(20, 10);

        assertThatThrownBy(() -> PaymentDeadlineCalculator.calculate(card, null))
                .isInstanceOf(CardNotValidException.class);
    }

    @Test
    @DisplayName("should throw when the card type is null")
    void calculate_ShouldThrow_WhenCardTypeIsNull() {
        Card card = Card.builder()
                .closingDay(20)
                .repaymentDay(10)
                .build();

        assertThatThrownBy(() -> PaymentDeadlineCalculator.calculate(card, LocalDate.of(2026, 3, 15)))
                .isInstanceOf(CardNotValidException.class);
    }

    private Card creditCard(Integer closingDay, Integer repaymentDay) {
        return Card.builder()
                .cardType(CardType.CREDIT)
                .closingDay(closingDay)
                .repaymentDay(repaymentDay)
                .build();
    }

    private Card deferredPaymentCard(Integer paymentTermDays) {
        return Card.builder()
                .cardType(CardType.DEFERRED_PAYMENT)
                .paymentTermDays(paymentTermDays)
                .build();
    }
}
