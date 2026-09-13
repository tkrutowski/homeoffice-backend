package net.focik.homeoffice.finance.domain.loan;

import net.focik.homeoffice.finance.api.mapper.ApiLoanMapper;
import net.focik.homeoffice.finance.domain.purchase.Purchase;
import net.focik.homeoffice.finance.domain.purchase.port.primary.GetPurchaseUseCase;
import net.focik.homeoffice.finance.domain.purchase.port.primary.UpdatePurchaseUseCase;
import net.focik.homeoffice.finance.infrastructure.jpa.BankTransactionDtoRepository;
import net.focik.homeoffice.userservice.domain.UserFacade;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Skupiony na zachowaniu wprowadzonym przy okazji ConvertPurchasesToLoanUseCase: usuniecie Loan
 * musi odlinkowac Purchase, ktore zostaly do niego wczesniej "wchloniete" (idLoan), zeby nie
 * zostaly osierocone (FK na nieistniejacy Loan) i wrocily jako samodzielne zakupy do zaplaty.
 */
@ExtendWith(MockitoExtension.class)
class LoanFacadeTest {

    @Mock
    private LoanService loanService;
    @Mock
    private UserFacade userFacade;
    @Mock
    private BankTransactionDtoRepository bankTransactionRepository;
    @Mock
    private ApiLoanMapper apiLoanMapper;
    @Mock
    private GetPurchaseUseCase getPurchaseUseCase;
    @Mock
    private UpdatePurchaseUseCase updatePurchaseUseCase;

    private LoanFacade loanFacade;

    @BeforeEach
    void setUp() {
        loanFacade = new LoanFacade(loanService, userFacade, bankTransactionRepository, apiLoanMapper,
                getPurchaseUseCase, updatePurchaseUseCase);
    }

    private Purchase convertedPurchase(int id, int idLoan) {
        return Purchase.builder()
                .id(id)
                .idUser(10)
                .idCard(1)
                .idFirm(1)
                .name("zakup-" + id)
                .purchaseDate(LocalDate.now())
                .paymentStatus(PaymentStatus.CONVERTED)
                .idLoan(idLoan)
                .build();
    }

    @Test
    void deleteLoanById_ShouldUnlinkAndRevertConvertedPurchases_BeforeDeletingLoan() {
        Purchase p1 = convertedPurchase(1, 99);
        Purchase p2 = convertedPurchase(2, 99);
        when(getPurchaseUseCase.findByLoan(99)).thenReturn(List.of(p1, p2));

        loanFacade.deleteLoanById(99);

        ArgumentCaptor<Purchase> captor = ArgumentCaptor.forClass(Purchase.class);
        verify(updatePurchaseUseCase, times(2)).updatePurchase(captor.capture());

        assertThat(captor.getAllValues())
                .allSatisfy(purchase -> {
                    assertThat(purchase.getIdLoan()).isNull();
                    assertThat(purchase.getPaymentStatus()).isEqualTo(PaymentStatus.TO_PAY);
                });

        verify(loanService, times(1)).deleteLoan(99);
    }

    @Test
    void deleteLoanById_ShouldNotTouchPurchases_WhenNoneAreLinked() {
        when(getPurchaseUseCase.findByLoan(5)).thenReturn(List.of());

        loanFacade.deleteLoanById(5);

        verify(updatePurchaseUseCase, never()).updatePurchase(any());
        verify(loanService, times(1)).deleteLoan(5);
    }
}
