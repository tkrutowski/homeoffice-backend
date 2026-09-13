package net.focik.homeoffice.finance.domain.loan;

import net.focik.homeoffice.finance.domain.exception.LoanAmountMismatchException;
import net.focik.homeoffice.finance.domain.exception.PurchaseAlreadyLinkedToLoanException;
import net.focik.homeoffice.finance.domain.exception.PurchaseAlreadyPaidException;
import net.focik.homeoffice.finance.domain.exception.PurchaseNotFoundException;
import net.focik.homeoffice.finance.domain.exception.PurchaseNotValidException;
import net.focik.homeoffice.finance.domain.exception.PurchaseUserMismatchException;
import net.focik.homeoffice.finance.domain.purchase.Purchase;
import net.focik.homeoffice.finance.domain.purchase.port.primary.GetPurchaseUseCase;
import net.focik.homeoffice.finance.domain.purchase.port.primary.UpdatePurchaseUseCase;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseLoanConversionFacadeTest {

    @Mock
    private LoanService loanService;

    @Mock
    private GetPurchaseUseCase getPurchaseUseCase;

    @Mock
    private UpdatePurchaseUseCase updatePurchaseUseCase;

    private PurchaseLoanConversionFacade facade;

    @BeforeEach
    void setUp() {
        facade = new PurchaseLoanConversionFacade(loanService, getPurchaseUseCase, updatePurchaseUseCase);
    }

    private Purchase purchase(Integer id, int idUser, int idCard, int idFirm, BigDecimal amount,
                               PaymentStatus status, LocalDate purchaseDate, Integer idLoan) {
        return Purchase.builder()
                .id(id)
                .idUser(idUser)
                .idCard(idCard)
                .idFirm(idFirm)
                .name("zakup-" + id)
                .amount(amount)
                .purchaseDate(purchaseDate)
                .paymentStatus(status)
                .idLoan(idLoan)
                .build();
    }

    private Loan loanData(int idUser, BigDecimal amount) {
        return Loan.builder()
                .idUser(idUser)
                .name("PayPo")
                .amount(Money.of(amount, "PLN"))
                .installmentAmount(Money.of(amount, "PLN"))
                .loanCost(Money.of(BigDecimal.ZERO, "PLN"))
                .date(LocalDate.now())
                .firstPaymentDate(LocalDate.now().plusMonths(1))
                .numberOfInstallments(1)
                .build();
    }

    // ---------- suggestLoanFromPurchases ----------

    @Test
    void suggest_ShouldSumAmountsAndPickLatestPurchaseDate() {
        Purchase p1 = purchase(1, 10, 1, 1, new BigDecimal("100.00"), PaymentStatus.TO_PAY, LocalDate.of(2026, 1, 5), null);
        Purchase p2 = purchase(2, 10, 1, 1, new BigDecimal("50.50"), PaymentStatus.TO_PAY, LocalDate.of(2026, 2, 10), null);
        when(getPurchaseUseCase.findAllById(List.of(1, 2))).thenReturn(List.of(p1, p2));

        LoanFromPurchasesDraft draft = facade.suggestLoanFromPurchases(List.of(1, 2));

        assertThat(draft.getSuggestedAmount()).isEqualByComparingTo(new BigDecimal("150.50"));
        assertThat(draft.getSuggestedDate()).isEqualTo(LocalDate.of(2026, 2, 10));
        assertThat(draft.getPurchases()).containsExactlyInAnyOrder(p1, p2);
        assertThat(draft.getWarnings()).isEmpty();
    }

    @Test
    void suggest_ShouldThrow_WhenPurchaseIdsEmpty() {
        assertThatThrownBy(() -> facade.suggestLoanFromPurchases(List.of()))
                .isInstanceOf(PurchaseNotValidException.class);
    }

    @Test
    void suggest_ShouldReportWarnings_WithoutBlocking_WhenSelectionIsRisky() {
        Purchase differentCard = purchase(1, 10, 1, 1, new BigDecimal("10.00"), PaymentStatus.TO_PAY, LocalDate.now(), null);
        Purchase alreadyPaid = purchase(2, 10, 2, 1, new BigDecimal("20.00"), PaymentStatus.PAID, LocalDate.now(), null);
        when(getPurchaseUseCase.findAllById(List.of(1, 2))).thenReturn(List.of(differentCard, alreadyPaid));

        LoanFromPurchasesDraft draft = facade.suggestLoanFromPurchases(List.of(1, 2));

        assertThat(draft.getWarnings()).isNotEmpty();
    }

    // ---------- convertPurchasesToLoan ----------

    @Test
    void convert_ShouldCreateLoanAndLinkSinglePurchase_LikePayPo() {
        Purchase purchase = purchase(1, 10, 1, 1, new BigDecimal("300.00"), PaymentStatus.TO_PAY, LocalDate.now(), null);
        Loan loanData = loanData(10, new BigDecimal("300.00"));
        Loan savedLoan = Loan.builder().id(99).idUser(10).amount(loanData.getAmount()).build();

        when(getPurchaseUseCase.findAllById(List.of(1))).thenReturn(List.of(purchase));
        when(loanService.saveLoan(loanData)).thenReturn(savedLoan);

        Loan result = facade.convertPurchasesToLoan(List.of(1), loanData);

        assertThat(result.getId()).isEqualTo(99);

        ArgumentCaptor<Purchase> captor = ArgumentCaptor.forClass(Purchase.class);
        verify(updatePurchaseUseCase, times(1)).updatePurchase(captor.capture());
        Purchase updated = captor.getValue();
        assertThat(updated.getIdLoan()).isEqualTo(99);
        assertThat(updated.getPaymentStatus()).isEqualTo(PaymentStatus.CONVERTED);
    }

    @Test
    void convert_ShouldCreateLoanAndLinkAllPurchases_LikeAllegroInstallments() {
        Purchase p1 = purchase(1, 10, 1, 1, new BigDecimal("100.00"), PaymentStatus.TO_PAY, LocalDate.now(), null);
        Purchase p2 = purchase(2, 10, 2, 1, new BigDecimal("200.00"), PaymentStatus.TO_PAY, LocalDate.now(), null);
        Loan loanData = loanData(10, new BigDecimal("300.00"));
        Loan savedLoan = Loan.builder().id(42).idUser(10).amount(loanData.getAmount()).build();

        when(getPurchaseUseCase.findAllById(List.of(1, 2))).thenReturn(List.of(p1, p2));
        when(loanService.saveLoan(loanData)).thenReturn(savedLoan);

        Loan result = facade.convertPurchasesToLoan(List.of(1, 2), loanData);

        assertThat(result.getId()).isEqualTo(42);
        verify(updatePurchaseUseCase, times(2)).updatePurchase(any(Purchase.class));
    }

    @Test
    void convert_ShouldAcceptRoundingTolerance_OfOneGrosz() {
        Purchase purchase = purchase(1, 10, 1, 1, new BigDecimal("100.00"), PaymentStatus.TO_PAY, LocalDate.now(), null);
        Loan loanData = loanData(10, new BigDecimal("100.01"));

        when(getPurchaseUseCase.findAllById(List.of(1))).thenReturn(List.of(purchase));
        when(loanService.saveLoan(loanData)).thenReturn(Loan.builder().id(1).idUser(10).build());

        assertThatCode(() -> facade.convertPurchasesToLoan(List.of(1), loanData)).doesNotThrowAnyException();
    }

    @Test
    void convert_ShouldThrow_WhenAmountsDontMatch() {
        Purchase purchase = purchase(1, 10, 1, 1, new BigDecimal("100.00"), PaymentStatus.TO_PAY, LocalDate.now(), null);
        Loan loanData = loanData(10, new BigDecimal("150.00"));
        when(getPurchaseUseCase.findAllById(List.of(1))).thenReturn(List.of(purchase));

        assertThatThrownBy(() -> facade.convertPurchasesToLoan(List.of(1), loanData))
                .isInstanceOf(LoanAmountMismatchException.class);

        verify(loanService, never()).saveLoan(any());
        verify(updatePurchaseUseCase, never()).updatePurchase(any());
    }

    @Test
    void convert_ShouldThrow_WhenPurchaseAlreadyLinkedToAnotherLoan() {
        Purchase purchase = purchase(1, 10, 1, 1, new BigDecimal("100.00"), PaymentStatus.TO_PAY, LocalDate.now(), 5);
        Loan loanData = loanData(10, new BigDecimal("100.00"));
        when(getPurchaseUseCase.findAllById(List.of(1))).thenReturn(List.of(purchase));

        assertThatThrownBy(() -> facade.convertPurchasesToLoan(List.of(1), loanData))
                .isInstanceOf(PurchaseAlreadyLinkedToLoanException.class);

        verify(loanService, never()).saveLoan(any());
    }

    @Test
    void convert_ShouldThrow_WhenPurchaseAlreadyPaid() {
        Purchase purchase = purchase(1, 10, 1, 1, new BigDecimal("100.00"), PaymentStatus.PAID, LocalDate.now(), null);
        Loan loanData = loanData(10, new BigDecimal("100.00"));
        when(getPurchaseUseCase.findAllById(List.of(1))).thenReturn(List.of(purchase));

        assertThatThrownBy(() -> facade.convertPurchasesToLoan(List.of(1), loanData))
                .isInstanceOf(PurchaseAlreadyPaidException.class);

        verify(loanService, never()).saveLoan(any());
    }

    @Test
    void convert_ShouldThrow_WhenPurchasesBelongToDifferentUsers() {
        Purchase p1 = purchase(1, 10, 1, 1, new BigDecimal("100.00"), PaymentStatus.TO_PAY, LocalDate.now(), null);
        Purchase p2 = purchase(2, 11, 1, 1, new BigDecimal("100.00"), PaymentStatus.TO_PAY, LocalDate.now(), null);
        Loan loanData = loanData(10, new BigDecimal("200.00"));
        when(getPurchaseUseCase.findAllById(List.of(1, 2))).thenReturn(List.of(p1, p2));

        assertThatThrownBy(() -> facade.convertPurchasesToLoan(List.of(1, 2), loanData))
                .isInstanceOf(PurchaseUserMismatchException.class);

        verify(loanService, never()).saveLoan(any());
    }

    @Test
    void convert_ShouldThrow_WhenLoanUserDiffersFromPurchasesUser() {
        Purchase purchase = purchase(1, 10, 1, 1, new BigDecimal("100.00"), PaymentStatus.TO_PAY, LocalDate.now(), null);
        Loan loanData = loanData(99, new BigDecimal("100.00"));
        when(getPurchaseUseCase.findAllById(List.of(1))).thenReturn(List.of(purchase));

        assertThatThrownBy(() -> facade.convertPurchasesToLoan(List.of(1), loanData))
                .isInstanceOf(PurchaseUserMismatchException.class);
    }

    @Test
    void convert_ShouldThrow_WhenPurchaseIdDoesNotExist() {
        Purchase purchase = purchase(1, 10, 1, 1, new BigDecimal("100.00"), PaymentStatus.TO_PAY, LocalDate.now(), null);
        Loan loanData = loanData(10, new BigDecimal("200.00"));
        when(getPurchaseUseCase.findAllById(List.of(1, 2))).thenReturn(List.of(purchase));

        assertThatThrownBy(() -> facade.convertPurchasesToLoan(List.of(1, 2), loanData))
                .isInstanceOf(PurchaseNotFoundException.class);
    }

    @Test
    void convert_ShouldThrow_WhenPurchaseIdsEmpty() {
        Loan loanData = loanData(10, BigDecimal.ZERO);

        assertThatThrownBy(() -> facade.convertPurchasesToLoan(List.of(), loanData))
                .isInstanceOf(PurchaseNotValidException.class);

        verify(getPurchaseUseCase, never()).findAllById(anyList());
    }
}
