package net.focik.homeoffice.library.domain;

import net.focik.homeoffice.emailservice.domain.EmailNotificationPort;
import net.focik.homeoffice.emailservice.domain.EmailRequest;
import net.focik.homeoffice.library.domain.model.ReadingStatus;
import net.focik.homeoffice.library.domain.model.UserBook;
import net.focik.homeoffice.library.domain.port.secondary.BookRepository;
import net.focik.homeoffice.library.domain.port.secondary.UserBookRepository;
import net.focik.homeoffice.fileService.domain.port.secondary.FileRepository;
import net.focik.homeoffice.userservice.domain.AppUser;
import net.focik.homeoffice.userservice.domain.UserFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BookService email notification methods.
 *
 * Tests cover:
 * - Monthly books summary email sending
 * - Yearly reading statistics email sending
 * - Error handling and edge cases
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BookService - Email Notifications")
class BookServiceEmailTest {

    // ============ Mocks ============
    @Mock
    private BookRepository bookRepository;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private UserBookRepository userBookRepository;

    @Mock
    private EmailNotificationPort emailNotificationPort;

    @Mock
    private UserFacade userFacade;

    // ============ Subject Under Test ============
    private BookService bookService;

    // ============ Test Data ============
    private AppUser testUser;
    private UserBook testUserBook1;
    private UserBook testUserBook2;

    @BeforeEach
    void setUp() {
        // Create service with mocks
        bookService = new BookService(
                bookRepository,
                fileRepository,
                userBookRepository,
                emailNotificationPort,
                userFacade
        );

        // Setup test data
        setupTestData();
    }

    private void setupTestData() {
        testUser = new AppUser();
        testUser.setId(1L);
        testUser.setFirstName("Jan");
        testUser.setLastName("Kowalski");
        testUser.setEmail("jan@example.com");
        testUser.setUsername("jkowalski");

        testUserBook1 = new UserBook();
        testUserBook1.setId(1);
        testUserBook1.setReadingStatus(ReadingStatus.READ);
        testUserBook1.setReadTo(LocalDate.now().minusDays(5));

        testUserBook2 = new UserBook();
        testUserBook2.setId(2);
        testUserBook2.setReadingStatus(ReadingStatus.READ);
        testUserBook2.setReadTo(LocalDate.now().minusDays(10));
    }

    // ============ Tests: sendMonthlyBooksSummary ============

    @Test
    @DisplayName("Should send monthly books summary when user has read books")
    void testSendMonthlySummary_WithBooks() {
        // Arrange
        Long userId = 1L;
        LocalDate now = LocalDate.now();
        LocalDate previousMonth = now.minusMonths(1);
        LocalDate monthStart = previousMonth.withDayOfMonth(1);
        LocalDate monthEnd = previousMonth.withDayOfMonth(previousMonth.lengthOfMonth());

        when(userFacade.findUserById(userId)).thenReturn(testUser);
        when(userBookRepository.findAllByUserAndReadStatusAndYear(
                userId,
                ReadingStatus.READ,
                monthStart,
                monthEnd
        )).thenReturn(Arrays.asList(testUserBook1, testUserBook2));

        // Act
        bookService.sendMonthlyBooksSummary(userId);

        // Assert
        ArgumentCaptor<EmailRequest> emailCaptor = ArgumentCaptor.forClass(EmailRequest.class);
        verify(emailNotificationPort, times(1)).sendTemplatedEmail(emailCaptor.capture());

        EmailRequest capturedEmail = emailCaptor.getValue();
        assertEquals(testUser.getEmail(), capturedEmail.getTo());
        assertEquals("monthly-books-summary", capturedEmail.getTemplateName());
        assertEquals(2, capturedEmail.getTemplateVariables().get("booksCount"));
    }

    @Test
    @DisplayName("Should not send email when user has no books read")
    void testSendMonthlySummary_NoBooks() {
        // Arrange
        Long userId = 1L;
        LocalDate now = LocalDate.now();
        LocalDate previousMonth = now.minusMonths(1);
        LocalDate monthStart = previousMonth.withDayOfMonth(1);
        LocalDate monthEnd = previousMonth.withDayOfMonth(previousMonth.lengthOfMonth());

        when(userFacade.findUserById(userId)).thenReturn(testUser);
        when(userBookRepository.findAllByUserAndReadStatusAndYear(
                userId,
                ReadingStatus.READ,
                monthStart,
                monthEnd
        )).thenReturn(new ArrayList<>());

        // Act
        bookService.sendMonthlyBooksSummary(userId);

        // Assert - email should still be sent (empty summary)
        ArgumentCaptor<EmailRequest> emailCaptor = ArgumentCaptor.forClass(EmailRequest.class);
        verify(emailNotificationPort, times(1)).sendTemplatedEmail(emailCaptor.capture());

        EmailRequest capturedEmail = emailCaptor.getValue();
        assertEquals(0, capturedEmail.getTemplateVariables().get("booksCount"));
    }

    @Test
    @DisplayName("Should skip email when user not found")
    void testSendMonthlySummary_UserNotFound() {
        // Arrange
        Long userId = 999L;

        when(userFacade.findUserById(userId)).thenReturn(null);

        // Act
        bookService.sendMonthlyBooksSummary(userId);

        // Assert - no email sent
        verify(emailNotificationPort, never()).sendTemplatedEmail(any());
    }

    @Test
    @DisplayName("Should skip email when user has no email address")
    void testSendMonthlySummary_NoEmail() {
        // Arrange
        Long userId = 1L;
        AppUser userNoEmail = new AppUser();
        userNoEmail.setId(userId);
        userNoEmail.setFirstName("John");
        userNoEmail.setEmail(null);

        when(userFacade.findUserById(userId)).thenReturn(userNoEmail);

        // Act
        bookService.sendMonthlyBooksSummary(userId);

        // Assert - no email sent
        verify(emailNotificationPort, never()).sendTemplatedEmail(any());
    }

    @Test
    @DisplayName("Should handle exceptions gracefully")
    void testSendMonthlySummary_ExceptionHandling() {
        // Arrange
        Long userId = 1L;

        when(userFacade.findUserById(userId)).thenThrow(new RuntimeException("Database error"));

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> bookService.sendMonthlyBooksSummary(userId));

        // Email should not be sent
        verify(emailNotificationPort, never()).sendTemplatedEmail(any());
    }

    // ============ Tests: sendYearlyReadingStatistics ============

    @Test
    @DisplayName("Should send yearly reading statistics")
    void testSendYearlyStatistics_Success() {
        // Arrange
        Long userId = 1L;
        Integer year = 2026;
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        when(userFacade.findUserById(userId)).thenReturn(testUser);
        when(userBookRepository.findAllByUserAndReadStatusAndYear(
                userId,
                ReadingStatus.READ,
                yearStart,
                yearEnd
        )).thenReturn(Arrays.asList(testUserBook1, testUserBook2));

        // Act
        bookService.sendYearlyReadingStatistics(userId, year);

        // Assert
        ArgumentCaptor<EmailRequest> emailCaptor = ArgumentCaptor.forClass(EmailRequest.class);
        verify(emailNotificationPort, times(1)).sendTemplatedEmail(emailCaptor.capture());

        EmailRequest capturedEmail = emailCaptor.getValue();
        assertEquals(testUser.getEmail(), capturedEmail.getTo());
        assertEquals("yearly-reading-statistics", capturedEmail.getTemplateName());
        assertTrue(capturedEmail.getTemplateVariables().containsKey("year"));
        assertEquals(year, capturedEmail.getTemplateVariables().get("year"));
    }

    @Test
    @DisplayName("Should calculate correct statistics")
    void testSendYearlyStatistics_VerifyCalculations() {
        // Arrange
        Long userId = 1L;
        Integer year = 2026;
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        List<UserBook> books = Arrays.asList(testUserBook1, testUserBook2);

        when(userFacade.findUserById(userId)).thenReturn(testUser);
        when(userBookRepository.findAllByUserAndReadStatusAndYear(
                userId,
                ReadingStatus.READ,
                yearStart,
                yearEnd
        )).thenReturn(books);

        // Act
        bookService.sendYearlyReadingStatistics(userId, year);

        // Assert
        ArgumentCaptor<EmailRequest> emailCaptor = ArgumentCaptor.forClass(EmailRequest.class);
        verify(emailNotificationPort).sendTemplatedEmail(emailCaptor.capture());

        EmailRequest capturedEmail = emailCaptor.getValue();
        Map<String, Object> vars = capturedEmail.getTemplateVariables();

        assertEquals(2, vars.get("totalBooksRead"));
        assertEquals(0, vars.get("averageBooksPerMonth")); // 2/12 rounded down
    }

    @Test
    @DisplayName("Should skip email for yearly statistics when user has no email")
    void testSendYearlyStatistics_NoEmail() {
        // Arrange
        Long userId = 1L;
        Integer year = 2026;
        AppUser userNoEmail = new AppUser();
        userNoEmail.setEmail(null);

        when(userFacade.findUserById(userId)).thenReturn(userNoEmail);

        // Act
        bookService.sendYearlyReadingStatistics(userId, year);

        // Assert
        verify(emailNotificationPort, never()).sendTemplatedEmail(any());
    }

    // ============ Tests: sendMonthlySummariesToAllUsers ============

    @Test
    @DisplayName("Should send summaries to all users")
    void testSendMonthlySummariesToAllUsers() {
        // Arrange
        AppUser user1 = new AppUser();
        user1.setId(1L);
        user1.setEmail("user1@example.com");

        AppUser user2 = new AppUser();
        user2.setId(2L);
        user2.setEmail("user2@example.com");

        List<AppUser> allUsers = Arrays.asList(user1, user2);

        when(userFacade.getAllUsers()).thenReturn(allUsers);
        when(userFacade.findUserById(1L)).thenReturn(user1);
        when(userFacade.findUserById(2L)).thenReturn(user2);
        when(userBookRepository.findAllByUserAndReadStatusAndYear(
                anyLong(),
                eq(ReadingStatus.READ),
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(new ArrayList<>());

        // Act
        bookService.sendMonthlySummariesToAllUsers();

        // Assert - should call sendTemplatedEmail twice (once for each user)
        verify(emailNotificationPort, times(2)).sendTemplatedEmail(any());
    }

    @Test
    @DisplayName("Should handle exception in scheduled method")
    void testSendMonthlySummariesToAllUsers_ExceptionHandling() {
        // Arrange
        when(userFacade.getAllUsers()).thenThrow(new RuntimeException("Database error"));

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> bookService.sendMonthlySummariesToAllUsers());

        // No emails should be sent
        verify(emailNotificationPort, never()).sendTemplatedEmail(any());
    }

    // ============ Tests: Template Variables Verification ============

    @Test
    @DisplayName("Should include correct template variables in email request")
    void testTemplateVariables_MonthlySummary() {
        // Arrange
        Long userId = 1L;
        LocalDate now = LocalDate.now();
        LocalDate previousMonth = now.minusMonths(1);
        LocalDate monthStart = previousMonth.withDayOfMonth(1);
        LocalDate monthEnd = previousMonth.withDayOfMonth(previousMonth.lengthOfMonth());

        when(userFacade.findUserById(userId)).thenReturn(testUser);
        when(userBookRepository.findAllByUserAndReadStatusAndYear(
                userId,
                ReadingStatus.READ,
                monthStart,
                monthEnd
        )).thenReturn(Arrays.asList(testUserBook1, testUserBook2));

        // Act
        bookService.sendMonthlyBooksSummary(userId);

        // Assert
        ArgumentCaptor<EmailRequest> emailCaptor = ArgumentCaptor.forClass(EmailRequest.class);
        verify(emailNotificationPort).sendTemplatedEmail(emailCaptor.capture());

        EmailRequest capturedEmail = emailCaptor.getValue();
        Map<String, Object> vars = capturedEmail.getTemplateVariables();

        // Verify required variables
        assertNotNull(vars.get("userName"));
        assertNotNull(vars.get("monthYear"));
        assertNotNull(vars.get("booksCount"));
        assertNotNull(vars.get("books"));
        assertNotNull(vars.get("currentYear"));

        assertEquals("Jan", vars.get("userName"));
        assertEquals(2, vars.get("booksCount"));
    }
}
