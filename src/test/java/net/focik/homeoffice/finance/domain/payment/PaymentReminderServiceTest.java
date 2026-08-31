package net.focik.homeoffice.finance.domain.payment;

import net.focik.homeoffice.emailservice.domain.EmailNotificationPort;
import net.focik.homeoffice.finance.domain.fee.Fee;
import net.focik.homeoffice.finance.domain.fee.FeeFacade;
import net.focik.homeoffice.finance.domain.fee.FeeInstallment;
import net.focik.homeoffice.finance.domain.loan.Loan;
import net.focik.homeoffice.finance.domain.loan.LoanFacade;
import net.focik.homeoffice.finance.domain.loan.LoanInstallment;
import net.focik.homeoffice.userservice.domain.AppUser;
import net.focik.homeoffice.userservice.domain.UserFacade;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentReminderServiceTest {

    @Mock
    private FeeFacade feeFacade;

    @Mock
    private LoanFacade loanFacade;

    @Mock
    private EmailNotificationPort emailNotificationPort;

    @Mock
    private UserFacade userFacade;

    @InjectMocks
    private PaymentReminderService paymentReminderService;

    private AppUser testUser;
    private Fee testFee;
    private Loan testLoan;

    @BeforeEach
    void setUp() {
        testUser = AppUser.builder()
                .id(1L)
                .firstName("Jan")
                .email("jan@example.com")
                .username("jan")
                .build();

        testFee = Fee.builder()
                .id(1)
                .idUser(1)
                .name("Test Fee")
                .feeStatus(PaymentStatus.TO_PAY)
                .build();

        testLoan = Loan.builder()
                .id(1)
                .idUser(1)
                .name("Test Loan")
                .loanStatus(PaymentStatus.TO_PAY)
                .build();
    }

    @Test
    void shouldNotSendReminderSevenDaysBeforeDeadline() {
        // Given
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(7);

        FeeInstallment installment = FeeInstallment.builder()
                .idFeeInstallment(1)
                .idFee(1)
                .paymentDeadline(deadline)
                .paymentStatus(PaymentStatus.TO_PAY)
                .installmentAmountToPay(Money.of(100, "PLN"))
                .build();

        testFee.setInstallments(List.of(installment));

        when(feeFacade.getFeesByStatus(PaymentStatus.TO_PAY, true)).thenReturn(List.of(testFee));
        when(loanFacade.getLoansByStatus(PaymentStatus.TO_PAY, true)).thenReturn(List.of());

        // When
        paymentReminderService.processPaymentReminders();

        // Then - nie wysyłamy przed terminem
        verify(emailNotificationPort, never()).sendTemplatedEmail(any());
    }

    @Test
    void shouldNotSendReminderThreeDaysBeforeDeadline() {
        // Given
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(3);

        FeeInstallment installment = FeeInstallment.builder()
                .idFeeInstallment(1)
                .idFee(1)
                .paymentDeadline(deadline)
                .paymentStatus(PaymentStatus.TO_PAY)
                .installmentAmountToPay(Money.of(100, "PLN"))
                .build();

        testFee.setInstallments(List.of(installment));

        when(feeFacade.getFeesByStatus(PaymentStatus.TO_PAY, true)).thenReturn(List.of(testFee));
        when(loanFacade.getLoansByStatus(PaymentStatus.TO_PAY, true)).thenReturn(List.of());

        // When
        paymentReminderService.processPaymentReminders();

        // Then - nie wysyłamy przed terminem
        verify(emailNotificationPort, never()).sendTemplatedEmail(any());
    }

    @Test
    void shouldNotSendReminderOneDayBeforeDeadline() {
        // Given
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(1);

        FeeInstallment installment = FeeInstallment.builder()
                .idFeeInstallment(1)
                .idFee(1)
                .paymentDeadline(deadline)
                .paymentStatus(PaymentStatus.TO_PAY)
                .installmentAmountToPay(Money.of(100, "PLN"))
                .build();

        testFee.setInstallments(List.of(installment));

        when(feeFacade.getFeesByStatus(PaymentStatus.TO_PAY, true)).thenReturn(List.of(testFee));
        when(loanFacade.getLoansByStatus(PaymentStatus.TO_PAY, true)).thenReturn(List.of());

        // When
        paymentReminderService.processPaymentReminders();

        // Then - nie wysyłamy przed terminem
        verify(emailNotificationPort, never()).sendTemplatedEmail(any());
    }

    @Test
    void shouldSendReminderOneDayAfterDeadline() {
        // Given - 1 dzień po terminie
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.minusDays(1);

        FeeInstallment installment = FeeInstallment.builder()
                .idFeeInstallment(1)
                .idFee(1)
                .paymentDeadline(deadline)
                .paymentStatus(PaymentStatus.TO_PAY)
                .installmentAmountToPay(Money.of(100, "PLN"))
                .build();

        testFee.setInstallments(List.of(installment));

        when(feeFacade.getFeesByStatus(PaymentStatus.TO_PAY, true)).thenReturn(List.of(testFee));
        when(loanFacade.getLoansByStatus(PaymentStatus.TO_PAY, true)).thenReturn(List.of());
        when(userFacade.findUserById(1L)).thenReturn(testUser);

        // When
        paymentReminderService.processPaymentReminders();

        // Then - wysyłamy 1 dzień po terminie
        verify(emailNotificationPort, times(1)).sendTemplatedEmail(any());
    }

    @Test
    void shouldSendReminderSevenDaysAfterDeadline() {
        // Given - 7 dni po terminie
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.minusDays(7);

        FeeInstallment installment = FeeInstallment.builder()
                .idFeeInstallment(1)
                .idFee(1)
                .paymentDeadline(deadline)
                .paymentStatus(PaymentStatus.TO_PAY)
                .installmentAmountToPay(Money.of(100, "PLN"))
                .build();

        testFee.setInstallments(List.of(installment));

        when(feeFacade.getFeesByStatus(PaymentStatus.TO_PAY, true)).thenReturn(List.of(testFee));
        when(loanFacade.getLoansByStatus(PaymentStatus.TO_PAY, true)).thenReturn(List.of());
        when(userFacade.findUserById(1L)).thenReturn(testUser);

        // When
        paymentReminderService.processPaymentReminders();

        // Then - wysyłamy 7 dni po terminie
        verify(emailNotificationPort, times(1)).sendTemplatedEmail(any());
    }

    @Test
    void shouldNotSendReminderForPaidInstallment() {
        // Given
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(7);

        FeeInstallment installment = FeeInstallment.builder()
                .idFeeInstallment(1)
                .idFee(1)
                .paymentDeadline(deadline)
                .paymentStatus(PaymentStatus.PAID)
                .installmentAmountToPay(Money.of(100, "PLN"))
                .build();

        testFee.setInstallments(List.of(installment));

        when(feeFacade.getFeesByStatus(PaymentStatus.TO_PAY, true)).thenReturn(List.of(testFee));
        when(loanFacade.getLoansByStatus(PaymentStatus.TO_PAY, true)).thenReturn(List.of());

        // When
        paymentReminderService.processPaymentReminders();

        // Then
        verify(emailNotificationPort, never()).sendTemplatedEmail(any());
    }

    @Test
    void shouldNotSendReminderTwoDaysBeforeDeadline() {
        // Given
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(2);

        FeeInstallment installment = FeeInstallment.builder()
                .idFeeInstallment(1)
                .idFee(1)
                .paymentDeadline(deadline)
                .paymentStatus(PaymentStatus.TO_PAY)
                .installmentAmountToPay(Money.of(100, "PLN"))
                .build();

        testFee.setInstallments(List.of(installment));

        when(feeFacade.getFeesByStatus(PaymentStatus.TO_PAY, true)).thenReturn(List.of(testFee));
        when(loanFacade.getLoansByStatus(PaymentStatus.TO_PAY, true)).thenReturn(List.of());

        // When
        paymentReminderService.processPaymentReminders();

        // Then
        verify(emailNotificationPort, never()).sendTemplatedEmail(any());
    }

    @Test
    void shouldNotSendReminderMoreThanSevenDaysAfterDeadline() {
        // Given - 8 dni po terminie (nie wysyłamy, tylko 1 i 7 dni po)
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.minusDays(8);

        FeeInstallment installment = FeeInstallment.builder()
                .idFeeInstallment(1)
                .idFee(1)
                .paymentDeadline(deadline)
                .paymentStatus(PaymentStatus.TO_PAY)
                .installmentAmountToPay(Money.of(100, "PLN"))
                .build();

        testFee.setInstallments(List.of(installment));

        when(feeFacade.getFeesByStatus(PaymentStatus.TO_PAY, true)).thenReturn(List.of(testFee));
        when(loanFacade.getLoansByStatus(PaymentStatus.TO_PAY, true)).thenReturn(List.of());

        // When
        paymentReminderService.processPaymentReminders();

        // Then - nie wysyłamy dla opóźnień poza 1 i 7 dni
        verify(emailNotificationPort, never()).sendTemplatedEmail(any());
    }

    @Test
    void shouldSendReminderForLoanInstallmentAfterDeadline() {
        // Given - 1 dzień po terminie
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.minusDays(1);

        LoanInstallment installment = LoanInstallment.builder()
                .idLoanInstallment(1)
                .idLoan(1)
                .paymentDeadline(deadline)
                .paymentStatus(PaymentStatus.TO_PAY)
                .installmentAmountToPay(Money.of(100, "PLN"))
                .build();

        testLoan.setInstallments(List.of(installment));

        when(feeFacade.getFeesByStatus(PaymentStatus.TO_PAY, true)).thenReturn(List.of());
        when(loanFacade.getLoansByStatus(PaymentStatus.TO_PAY, true)).thenReturn(List.of(testLoan));
        when(userFacade.findUserById(1L)).thenReturn(testUser);

        // When
        paymentReminderService.processPaymentReminders();

        // Then - wysyłamy dla opóźnionej pożyczki (1 dzień po terminie)
        verify(emailNotificationPort, times(1)).sendTemplatedEmail(any());
    }

    @Test
    void shouldNotSendReminderForLoanInstallmentBeforeDeadline() {
        // Given - 7 dni przed terminem
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(7);

        LoanInstallment installment = LoanInstallment.builder()
                .idLoanInstallment(1)
                .idLoan(1)
                .paymentDeadline(deadline)
                .paymentStatus(PaymentStatus.TO_PAY)
                .installmentAmountToPay(Money.of(100, "PLN"))
                .build();

        testLoan.setInstallments(List.of(installment));

        when(feeFacade.getFeesByStatus(PaymentStatus.TO_PAY, true)).thenReturn(List.of());
        when(loanFacade.getLoansByStatus(PaymentStatus.TO_PAY, true)).thenReturn(List.of(testLoan));

        // When
        paymentReminderService.processPaymentReminders();

        // Then - nie wysyłamy przed terminem
        verify(emailNotificationPort, never()).sendTemplatedEmail(any());
    }

    @Test
    void shouldNotSendReminderForNullEmail() {
        // Given - 1 dzień po terminie, ale email jest null
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.minusDays(1);

        FeeInstallment installment = FeeInstallment.builder()
                .idFeeInstallment(1)
                .idFee(1)
                .paymentDeadline(deadline)
                .paymentStatus(PaymentStatus.TO_PAY)
                .installmentAmountToPay(Money.of(100, "PLN"))
                .build();

        testFee.setInstallments(List.of(installment));
        testUser.setEmail(null);

        when(feeFacade.getFeesByStatus(PaymentStatus.TO_PAY, true)).thenReturn(List.of(testFee));
        when(loanFacade.getLoansByStatus(PaymentStatus.TO_PAY, true)).thenReturn(List.of());
        when(userFacade.findUserById(1L)).thenReturn(testUser);

        // When
        paymentReminderService.processPaymentReminders();

        // Then - nie wysyłamy email gdy użytkownik nie ma emaila
        verify(emailNotificationPort, never()).sendTemplatedEmail(any());
    }
}
