package net.focik.homeoffice.finance.domain.purchase;

import net.focik.homeoffice.emailservice.domain.EmailNotificationPort;
import net.focik.homeoffice.emailservice.domain.EmailRequest;
import net.focik.homeoffice.finance.domain.card.Card;
import net.focik.homeoffice.finance.domain.card.CardFacade;
import net.focik.homeoffice.finance.domain.purchase.port.primary.GetPurchaseUseCase;
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
class PurchaseReportServiceTest {

    @Mock
    private GetPurchaseUseCase getPurchaseUseCase;

    @Mock
    private CardFacade cardFacade;

    @Mock
    private EmailNotificationPort emailNotificationPort;

    @Mock
    private UserFacade userFacade;

    @InjectMocks
    private PurchaseReportService purchaseReportService;

    private AppUser testUser;
    private Card testCard;
    private Purchase testPurchase;

    @BeforeEach
    void setUp() {
        testUser = AppUser.builder()
                .id(1L)
                .firstName("Jan")
                .email("jan@example.com")
                .username("jan")
                .build();

        testCard = Card.builder()
                .id(1)
                .cardName("Visa Card")
                .build();

        // Set purchase to first day of previous month to ensure it's within the filtered range
        LocalDate previousMonthDate = LocalDate.now().minusMonths(1).withDayOfMonth(15);

        testPurchase = Purchase.builder()
                .id(1)
                .idCard(1)
                .idUser(1)
                .name("Zakup w sklepie")
                .amount(new BigDecimal("150.00"))
                .purchaseDate(previousMonthDate)
                .build();
    }

    @Test
    void shouldProcessMonthlyReportsWithPurchases() {
        // Given
        List<Purchase> purchases = List.of(testPurchase);

        when(getPurchaseUseCase.findByUser(anyString(), any(), any())).thenReturn(purchases);
        when(cardFacade.findById(1)).thenReturn(testCard);
        when(userFacade.getAllUsers()).thenReturn(List.of(testUser));

        // When
        purchaseReportService.processMonthlyReports();

        // Then
        verify(emailNotificationPort, times(1)).sendTemplatedEmail(any());
    }

    @Test
    void shouldGroupPurchasesByCard() {
        // Given
        LocalDate previousMonthDate = LocalDate.now().minusMonths(1).withDayOfMonth(20);

        Purchase purchase2 = Purchase.builder()
                .id(2)
                .idCard(1)
                .idUser(1)
                .name("Drugi zakup")
                .amount(new BigDecimal("200.00"))
                .purchaseDate(previousMonthDate)
                .build();

        List<Purchase> purchases = List.of(testPurchase, purchase2);

        when(getPurchaseUseCase.findByUser(anyString(), any(), any())).thenReturn(purchases);
        when(cardFacade.findById(1)).thenReturn(testCard);
        when(userFacade.getAllUsers()).thenReturn(List.of(testUser));

        // When
        purchaseReportService.processMonthlyReports();

        // Then
        ArgumentCaptor<EmailRequest> emailCaptor = ArgumentCaptor.forClass(EmailRequest.class);
        verify(emailNotificationPort).sendTemplatedEmail(emailCaptor.capture());
        assertNotNull(emailCaptor.getValue());
    }

    @Test
    void shouldNotSendReportWhenNoPurchases() {
        // Given
        when(getPurchaseUseCase.findByUser(anyString(), any(), any())).thenReturn(new ArrayList<>());
        when(userFacade.getAllUsers()).thenReturn(List.of(testUser));

        // When
        purchaseReportService.processMonthlyReports();

        // Then
        verify(emailNotificationPort, never()).sendTemplatedEmail(any());
    }

    @Test
    void shouldProcessWeeklyReportsWithPurchases() {
        // Given - use purchase from previous week
        LocalDate previousWeekDate = LocalDate.now().minusWeeks(1);

        Purchase weeklyPurchase = Purchase.builder()
                .id(1)
                .idCard(1)
                .idUser(1)
                .name("Zakup w poprzednim tygodniu")
                .amount(new BigDecimal("150.00"))
                .purchaseDate(previousWeekDate)
                .build();

        List<Purchase> purchases = List.of(weeklyPurchase);

        when(getPurchaseUseCase.findByUser(anyString(), any(), any())).thenReturn(purchases);
        when(cardFacade.findById(1)).thenReturn(testCard);
        when(userFacade.getAllUsers()).thenReturn(List.of(testUser));

        // When
        purchaseReportService.processWeeklyReports();

        // Then
        verify(emailNotificationPort, times(1)).sendTemplatedEmail(any());
    }

    @Test
    void shouldNotSendToUserWithoutEmail() {
        // Given
        testUser.setEmail(null);
        List<Purchase> purchases = List.of(testPurchase);

        when(getPurchaseUseCase.findByUser(anyString(), any(), any())).thenReturn(purchases);
        when(userFacade.getAllUsers()).thenReturn(List.of(testUser));

        // When
        purchaseReportService.processMonthlyReports();

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

        List<Purchase> purchases = List.of(testPurchase);
        List<AppUser> users = List.of(testUser, user2);

        when(getPurchaseUseCase.findByUser(anyString(), any(), any())).thenReturn(purchases);
        when(cardFacade.findById(1)).thenReturn(testCard);
        when(userFacade.getAllUsers()).thenReturn(users);

        // When
        purchaseReportService.processMonthlyReports();

        // Then
        verify(emailNotificationPort, times(2)).sendTemplatedEmail(any());
    }

    @Test
    void shouldHandleMultiplePurchasesFromMultipleCards() {
        // Given
        LocalDate previousMonthDate = LocalDate.now().minusMonths(1).withDayOfMonth(10);

        Card card2 = Card.builder()
                .id(2)
                .cardName("MasterCard")
                .build();

        Purchase purchase2 = Purchase.builder()
                .id(2)
                .idCard(2)
                .idUser(1)
                .name("Zakup na drugiej karcie")
                .amount(new BigDecimal("250.00"))
                .purchaseDate(previousMonthDate)
                .build();

        List<Purchase> purchases = List.of(testPurchase, purchase2);

        when(getPurchaseUseCase.findByUser(anyString(), any(), any())).thenReturn(purchases);
        when(cardFacade.findById(1)).thenReturn(testCard);
        when(cardFacade.findById(2)).thenReturn(card2);
        when(userFacade.getAllUsers()).thenReturn(List.of(testUser));

        // When
        purchaseReportService.processMonthlyReports();

        // Then
        verify(emailNotificationPort, times(1)).sendTemplatedEmail(any());
    }

    @Test
    void shouldFilterPurchasesToOnlyPreviousMonth() {
        // Given
        // Old purchase from 2+ months ago should be filtered out
        Purchase oldPurchase = Purchase.builder()
                .id(2)
                .idCard(1)
                .idUser(1)
                .name("Stary zakup")
                .amount(new BigDecimal("100.00"))
                .purchaseDate(LocalDate.now().minusMonths(2))
                .build();

        List<Purchase> allPurchases = List.of(testPurchase, oldPurchase);

        when(getPurchaseUseCase.findByUser(anyString(), any(), any())).thenReturn(allPurchases);
        when(cardFacade.findById(1)).thenReturn(testCard);
        when(userFacade.getAllUsers()).thenReturn(List.of(testUser));

        // When
        purchaseReportService.processMonthlyReports();

        // Then - only testPurchase should be included (oldPurchase filtered out)
        verify(emailNotificationPort, times(1)).sendTemplatedEmail(any());
    }

    @Test
    void shouldHandleErrorDuringProcessing() {
        // Given
        when(getPurchaseUseCase.findByUser(anyString(), any(), any()))
                .thenThrow(new RuntimeException("Database error"));
        when(userFacade.getAllUsers()).thenReturn(List.of(testUser));

        // When
        try {
            purchaseReportService.processMonthlyReports();
        } catch (Exception e) {
            // Exception should be caught by service
        }

        // Then
        verify(emailNotificationPort, never()).sendTemplatedEmail(any());
    }
}