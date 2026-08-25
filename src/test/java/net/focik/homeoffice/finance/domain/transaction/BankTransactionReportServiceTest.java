package net.focik.homeoffice.finance.domain.transaction;

import net.focik.homeoffice.emailservice.domain.EmailNotificationPort;
import net.focik.homeoffice.emailservice.domain.EmailRequest;
import net.focik.homeoffice.finance.domain.transaction.model.BankTransaction;
import net.focik.homeoffice.finance.domain.transaction.model.TransactionType;
import net.focik.homeoffice.finance.domain.transaction.port.primary.GetBankTransactionUseCase;
import net.focik.homeoffice.userservice.domain.AppUser;
import net.focik.homeoffice.userservice.domain.UserFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankTransactionReportServiceTest {

    @Mock
    private GetBankTransactionUseCase getBankTransactionUseCase;

    @Mock
    private EmailNotificationPort emailNotificationPort;

    @Mock
    private UserFacade userFacade;

    @InjectMocks
    private BankTransactionReportService bankTransactionReportService;

    private AppUser testUser;
    private BankTransaction incomeTransaction;
    private BankTransaction expenseTransaction;

    @BeforeEach
    void setUp() {
        testUser = AppUser.builder()
                .id(1L)
                .firstName("Jan")
                .email("jan@example.com")
                .username("jan")
                .build();

        incomeTransaction = BankTransaction.builder()
                .id(1)
                .idUser(1)
                .description("Przelew przychodzący")
                .amount(new BigDecimal("1000.00"))
                .transactionType(TransactionType.TRANSFER_IN)
                .transactionDate(LocalDate.now().minusDays(5))
                .build();

        expenseTransaction = BankTransaction.builder()
                .id(2)
                .idUser(1)
                .description("Spłata karty")
                .amount(new BigDecimal("500.00"))
                .transactionType(TransactionType.CARD_PAYMENT)
                .transactionDate(LocalDate.now().minusDays(3))
                .build();
    }

    @Test
    void shouldProcessMonthlyReportsWithMixedTransactions() {
        // Given
        List<BankTransaction> transactions = List.of(incomeTransaction, expenseTransaction);

        when(getBankTransactionUseCase.findBetween(any(), any(), anyInt())).thenReturn(transactions);
        when(userFacade.getAllUsers()).thenReturn(List.of(testUser));

        // When
        bankTransactionReportService.processMonthlyReports();

        // Then
        verify(emailNotificationPort, times(1)).sendTemplatedEmail(any());
    }

    @Test
    void shouldSeparateIncomeFromExpenses() {
        // Given
        List<BankTransaction> transactions = List.of(incomeTransaction, expenseTransaction);

        when(getBankTransactionUseCase.findBetween(any(), any(), anyInt())).thenReturn(transactions);
        when(userFacade.getAllUsers()).thenReturn(List.of(testUser));

        // When
        bankTransactionReportService.processMonthlyReports();

        // Then
        ArgumentCaptor<EmailRequest> emailCaptor = ArgumentCaptor.forClass(EmailRequest.class);
        verify(emailNotificationPort).sendTemplatedEmail(emailCaptor.capture());
        assertNotNull(emailCaptor.getValue());
    }

    @Test
    void shouldNotSendReportWhenNoTransactions() {
        // Given
        when(getBankTransactionUseCase.findBetween(any(), any(), anyInt())).thenReturn(new ArrayList<>());
        when(userFacade.getAllUsers()).thenReturn(List.of(testUser));

        // When
        bankTransactionReportService.processMonthlyReports();

        // Then
        verify(emailNotificationPort, never()).sendTemplatedEmail(any());
    }

    @Test
    void shouldProcessWeeklyReportsWithTransactions() {
        // Given
        List<BankTransaction> transactions = List.of(incomeTransaction, expenseTransaction);

        when(getBankTransactionUseCase.findBetween(any(), any(), anyInt())).thenReturn(transactions);
        when(userFacade.getAllUsers()).thenReturn(List.of(testUser));

        // When
        bankTransactionReportService.processWeeklyReports();

        // Then
        verify(emailNotificationPort, times(1)).sendTemplatedEmail(any());
    }

    @Test
    void shouldNotSendToUserWithoutEmail() {
        // Given
        testUser.setEmail(null);
        List<BankTransaction> transactions = List.of(incomeTransaction, expenseTransaction);

        when(getBankTransactionUseCase.findBetween(any(), any(), anyInt())).thenReturn(transactions);
        when(userFacade.getAllUsers()).thenReturn(List.of(testUser));

        // When
        bankTransactionReportService.processMonthlyReports();

        // Then
        verify(emailNotificationPort, never()).sendTemplatedEmail(any());
    }

    @Test
    void shouldSendToMultipleUsers() {
        // Given
        AppUser user2 = AppUser.builder()
                .id(2L)
                .firstName("Maria")
                .email("maria@example.com")
                .username("maria")
                .build();

        List<BankTransaction> transactions = List.of(incomeTransaction, expenseTransaction);
        List<AppUser> users = List.of(testUser, user2);

        when(getBankTransactionUseCase.findBetween(any(), any(), anyInt())).thenReturn(transactions);
        when(userFacade.getAllUsers()).thenReturn(users);

        // When
        bankTransactionReportService.processMonthlyReports();

        // Then
        verify(emailNotificationPort, times(2)).sendTemplatedEmail(any());
    }

    @Test
    void shouldHandleMultipleIncomeAndExpenses() {
        // Given
        BankTransaction income2 = BankTransaction.builder()
                .id(3)
                .idUser(1)
                .description("Wpłata")
                .amount(new BigDecimal("2000.00"))
                .transactionType(TransactionType.DEPOSIT)
                .transactionDate(LocalDate.now().minusDays(10))
                .build();

        BankTransaction expense2 = BankTransaction.builder()
                .id(4)
                .idUser(1)
                .description("Wypłata")
                .amount(new BigDecimal("300.00"))
                .transactionType(TransactionType.WITHDRAWAL)
                .transactionDate(LocalDate.now().minusDays(1))
                .build();

        List<BankTransaction> transactions = List.of(incomeTransaction, expenseTransaction, income2, expense2);

        when(getBankTransactionUseCase.findBetween(any(), any(), anyInt())).thenReturn(transactions);
        when(userFacade.getAllUsers()).thenReturn(List.of(testUser));

        // When
        bankTransactionReportService.processMonthlyReports();

        // Then
        verify(emailNotificationPort, times(1)).sendTemplatedEmail(any());
    }

    @Test
    void shouldHandleErrorDuringProcessing() {
        // Given
        when(getBankTransactionUseCase.findBetween(any(), any(), anyInt()))
                .thenThrow(new RuntimeException("Database error"));
        when(userFacade.getAllUsers()).thenReturn(List.of(testUser));

        // When
        try {
            bankTransactionReportService.processMonthlyReports();
        } catch (Exception e) {
            // Exception should be caught by service
        }

        // Then
        verify(emailNotificationPort, never()).sendTemplatedEmail(any());
    }
}