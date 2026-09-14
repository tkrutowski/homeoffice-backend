package net.focik.homeoffice.finance.domain.loan;

import net.focik.homeoffice.finance.api.mapper.ApiLoanMapper;
import net.focik.homeoffice.finance.domain.purchase.Purchase;
import net.focik.homeoffice.finance.domain.purchase.port.primary.GetPurchaseUseCase;
import net.focik.homeoffice.finance.domain.purchase.port.primary.UpdatePurchaseUseCase;
import net.focik.homeoffice.finance.infrastructure.jpa.BankTransactionDtoRepository;
import net.focik.homeoffice.userservice.domain.AppUser;
import net.focik.homeoffice.userservice.domain.UserFacade;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String username, String... authorities) {
        List<GrantedAuthority> grantedAuthorities = Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, grantedAuthorities));
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
    @DisplayName("deleteLoanById should unlink and revert converted purchases before deleting the loan")
    void deleteLoanById_ShouldUnlinkAndRevertConvertedPurchases_BeforeDeletingLoan() {
        when(loanService.findLoanById(99, false)).thenReturn(Loan.builder().id(99).idUser(7).build());
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
    @DisplayName("deleteLoanById should not touch any purchases when none are linked to the loan")
    void deleteLoanById_ShouldNotTouchPurchases_WhenNoneAreLinked() {
        when(loanService.findLoanById(5, false)).thenReturn(Loan.builder().id(5).idUser(7).build());
        when(getPurchaseUseCase.findByLoan(5)).thenReturn(List.of());

        loanFacade.deleteLoanById(5);

        verify(updatePurchaseUseCase, never()).updatePurchase(any());
        verify(loanService, times(1)).deleteLoan(5);
    }

    @Test
    @DisplayName("deleteLoanById should throw access denied and not touch purchases when the requesting user is not the owner and lacks DELETE_ALL")
    void deleteLoanById_ShouldThrowAccessDenied_WhenRequestingUserIsNotOwnerAndHasNoDeleteAllPrivilege() {
        authenticateAs("john", "ROLE_FINANCE");
        when(loanService.findLoanById(99, false)).thenReturn(Loan.builder().id(99).idUser(7).build());
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(99L).build());

        assertThatThrownBy(() -> loanFacade.deleteLoanById(99))
                .isInstanceOf(AccessDeniedException.class);

        verify(loanService, never()).deleteLoan(anyInt());
        verify(getPurchaseUseCase, never()).findByLoan(anyInt());
    }

    @Test
    @DisplayName("deleteLoanById should throw access denied when the user has only the WRITE_ALL authority, not DELETE_ALL")
    void deleteLoanById_ShouldThrowAccessDenied_WhenUserHasOnlyWriteAllAuthority() {
        // WRITE_ALL nie uprawnia do usuwania cudzych kredytow - do tego sluzy osobne DELETE_ALL
        authenticateAs("john", "FINANCE_LOAN_WRITE_ALL");
        when(loanService.findLoanById(99, false)).thenReturn(Loan.builder().id(99).idUser(7).build());
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(99L).build());

        assertThatThrownBy(() -> loanFacade.deleteLoanById(99))
                .isInstanceOf(AccessDeniedException.class);

        verify(loanService, never()).deleteLoan(anyInt());
    }

    @Test
    @DisplayName("deleteLoanById should delete the loan when the requesting user is its owner")
    void deleteLoanById_ShouldDeleteOwnLoan_WhenRequestingUserIsOwner() {
        authenticateAs("john", "ROLE_FINANCE");
        when(loanService.findLoanById(99, false)).thenReturn(Loan.builder().id(99).idUser(7).build());
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(7L).build());
        when(getPurchaseUseCase.findByLoan(99)).thenReturn(List.of());

        loanFacade.deleteLoanById(99);

        verify(loanService).deleteLoan(99);
    }

    @Test
    @DisplayName("deleteLoanById should delete any loan, even someone else's, when the user has the DELETE_ALL authority")
    void deleteLoanById_ShouldDeleteAnyLoan_WhenUserHasDeleteAllAuthority() {
        authenticateAs("admin", "FINANCE_LOAN_DELETE_ALL");
        when(loanService.findLoanById(99, false)).thenReturn(Loan.builder().id(99).idUser(7).build());
        when(getPurchaseUseCase.findByLoan(99)).thenReturn(List.of());

        loanFacade.deleteLoanById(99);

        verify(loanService).deleteLoan(99);
        verify(userFacade, never()).findUserByUsername(any());
    }

    @Test
    @DisplayName("deleteLoanInstallmentById should throw access denied when the requesting user does not own the parent loan and lacks DELETE_ALL")
    void deleteLoanInstallmentById_ShouldThrowAccessDenied_WhenRequestingUserIsNotOwnerAndHasNoDeleteAllPrivilege() {
        authenticateAs("john", "ROLE_FINANCE");
        LoanInstallment installment = LoanInstallment.builder().idLoanInstallment(5).idLoan(99).build();
        when(loanService.getLoanInstallment(5)).thenReturn(installment);
        when(loanService.findLoanById(99, false)).thenReturn(Loan.builder().id(99).idUser(7).build());
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(99L).build());

        assertThatThrownBy(() -> loanFacade.deleteLoanInstallmentById(5))
                .isInstanceOf(AccessDeniedException.class);

        verify(loanService, never()).deleteLoanInstallment(anyInt());
    }

    @Test
    @DisplayName("deleteLoanInstallmentById should delete the installment when the requesting user owns the parent loan")
    void deleteLoanInstallmentById_ShouldDelete_WhenRequestingUserIsOwner() {
        authenticateAs("john", "ROLE_FINANCE");
        LoanInstallment installment = LoanInstallment.builder().idLoanInstallment(5).idLoan(99).build();
        when(loanService.getLoanInstallment(5)).thenReturn(installment);
        when(loanService.findLoanById(99, false)).thenReturn(Loan.builder().id(99).idUser(7).build());
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(7L).build());

        loanFacade.deleteLoanInstallmentById(5);

        verify(loanService).deleteLoanInstallment(5);
    }

    @Test
    @DisplayName("deleteLoanInstallmentById should delete the installment when the user has the DELETE_ALL authority")
    void deleteLoanInstallmentById_ShouldDelete_WhenUserHasDeleteAllAuthority() {
        authenticateAs("admin", "FINANCE_LOAN_DELETE_ALL");
        LoanInstallment installment = LoanInstallment.builder().idLoanInstallment(5).idLoan(99).build();
        when(loanService.getLoanInstallment(5)).thenReturn(installment);
        when(loanService.findLoanById(99, false)).thenReturn(Loan.builder().id(99).idUser(7).build());

        loanFacade.deleteLoanInstallmentById(5);

        verify(loanService).deleteLoanInstallment(5);
        verify(userFacade, never()).findUserByUsername(any());
    }

    // ---- Kontrola dostępu (na wzór getLoansByStatus) ----

    @Test
    @DisplayName("getLoanById should return the loan without an ownership check when there is no authentication context")
    void getLoanById_ShouldReturnLoan_WhenNoAuthenticationContext() {
        // brak kontekstu security (np. zadanie schedulera) - traktowane jak pelny dostep
        Loan loan = Loan.builder().id(1).idUser(5).build();
        when(loanService.findLoanById(1, true)).thenReturn(loan);

        Loan result = loanFacade.getLoanById(1, true);

        assertThat(result).isEqualTo(loan);
        verify(userFacade, never()).findUserByUsername(any());
    }

    @Test
    @DisplayName("getLoanById should return the loan when the requesting user is its owner")
    void getLoanById_ShouldReturnLoan_WhenRequestingUserIsOwner() {
        authenticateAs("john", "ROLE_FINANCE");
        Loan loan = Loan.builder().id(1).idUser(7).build();
        when(loanService.findLoanById(1, true)).thenReturn(loan);
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(7L).build());

        Loan result = loanFacade.getLoanById(1, true);

        assertThat(result).isEqualTo(loan);
    }

    @Test
    @DisplayName("getLoanById should throw access denied when the requesting user is not the owner and lacks READ_ALL")
    void getLoanById_ShouldThrowAccessDenied_WhenRequestingUserIsNotOwnerAndHasNoReadAllPrivilege() {
        authenticateAs("john", "ROLE_FINANCE");
        Loan loan = Loan.builder().id(1).idUser(7).build();
        when(loanService.findLoanById(1, true)).thenReturn(loan);
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(99L).build());

        assertThatThrownBy(() -> loanFacade.getLoanById(1, true))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("getLoanById should return any loan, even someone else's, when the user has the READ_ALL authority")
    void getLoanById_ShouldReturnAnyLoan_WhenUserHasReadAllAuthority() {
        authenticateAs("admin", "FINANCE_LOAN_READ_ALL");
        Loan loan = Loan.builder().id(1).idUser(7).build();
        when(loanService.findLoanById(1, true)).thenReturn(loan);

        Loan result = loanFacade.getLoanById(1, true);

        assertThat(result).isEqualTo(loan);
        verify(userFacade, never()).findUserByUsername(any());
    }

    @Test
    @DisplayName("getLoanById should throw access denied when the user has only the WRITE_ALL authority, not READ_ALL")
    void getLoanById_ShouldThrowAccessDenied_WhenUserHasOnlyWriteAllAuthority() {
        // READ_ALL i WRITE_ALL to celowo osobne uprawnienia - samo WRITE_ALL nie daje prawa do odczytu cudzych danych
        authenticateAs("john", "FINANCE_LOAN_WRITE_ALL");
        Loan loan = Loan.builder().id(1).idUser(7).build();
        when(loanService.findLoanById(1, true)).thenReturn(loan);
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(99L).build());

        assertThatThrownBy(() -> loanFacade.getLoanById(1, true))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("findLoansPageableWithFilters should override the requested idUser filter with the caller's own id when they lack READ_ALL")
    void findLoansPageableWithFilters_ShouldOverrideRequestedIdUser_WhenUserHasNoReadAllPrivilege() {
        authenticateAs("john", "ROLE_FINANCE");
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(7L).build());
        when(loanService.findLoansPageableWithFilters(
                eq(0), eq(20), eq("date"), eq("DESC"), isNull(), isNull(), isNull(),
                isNull(), eq("EQUALS"), isNull(), eq("EQUALS"), isNull(), eq(7)))
                .thenReturn(Page.empty());

        // klient probuje podejrzec cudze dane (idUser=999) - serwer ma to zignorowac
        loanFacade.findLoansPageableWithFilters(0, 20, "date", "DESC", null, null, null,
                null, "EQUALS", null, "EQUALS", null, 999);

        verify(loanService).findLoansPageableWithFilters(
                eq(0), eq(20), eq("date"), eq("DESC"), isNull(), isNull(), isNull(),
                isNull(), eq("EQUALS"), isNull(), eq("EQUALS"), isNull(), eq(7));
    }

    @Test
    @DisplayName("findLoansPageableWithFilters should keep the requested idUser filter when the user has the READ_ALL authority")
    void findLoansPageableWithFilters_ShouldKeepRequestedIdUser_WhenUserHasReadAllAuthority() {
        authenticateAs("admin", "ROLE_ADMIN");
        when(loanService.findLoansPageableWithFilters(
                eq(0), eq(20), eq("date"), eq("DESC"), isNull(), isNull(), isNull(),
                isNull(), eq("EQUALS"), isNull(), eq("EQUALS"), isNull(), eq(999)))
                .thenReturn(Page.empty());

        loanFacade.findLoansPageableWithFilters(0, 20, "date", "DESC", null, null, null,
                null, "EQUALS", null, "EQUALS", null, 999);

        verify(loanService).findLoansPageableWithFilters(
                eq(0), eq(20), eq("date"), eq("DESC"), isNull(), isNull(), isNull(),
                isNull(), eq("EQUALS"), isNull(), eq("EQUALS"), isNull(), eq(999));
        verify(userFacade, never()).findUserByUsername(any());
    }

    @Test
    @DisplayName("addLoan should override the requested idUser with the caller's own id when they lack WRITE_ALL")
    void addLoan_ShouldOverrideRequestedIdUser_WhenUserHasNoWriteAllPrivilege() {
        authenticateAs("john", "ROLE_FINANCE");
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(7L).build());
        // proba dodania kredytu "na konto" innej osoby (idUser=999)
        Loan loanToAdd = Loan.builder().idUser(999).build();
        when(loanService.saveLoan(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Loan result = loanFacade.addLoan(loanToAdd);

        assertThat(result.getIdUser()).isEqualTo(7);
    }

    @Test
    @DisplayName("addLoan should override the requested idUser when the user has only READ_ALL, since READ_ALL does not grant write access")
    void addLoan_ShouldOverrideRequestedIdUser_WhenUserHasOnlyReadAllAuthority() {
        // sam READ_ALL (bez WRITE_ALL) nie uprawnia do zakladania kredytow na cudze konto
        authenticateAs("john", "FINANCE_LOAN_READ_ALL");
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(7L).build());
        Loan loanToAdd = Loan.builder().idUser(999).build();
        when(loanService.saveLoan(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Loan result = loanFacade.addLoan(loanToAdd);

        assertThat(result.getIdUser()).isEqualTo(7);
    }

    @Test
    @DisplayName("addLoan should keep the requested idUser when the user has the WRITE_ALL authority")
    void addLoan_ShouldKeepRequestedIdUser_WhenUserHasWriteAllAuthority() {
        authenticateAs("admin", "FINANCE_LOAN_WRITE_ALL");
        Loan loanToAdd = Loan.builder().idUser(999).build();
        when(loanService.saveLoan(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Loan result = loanFacade.addLoan(loanToAdd);

        assertThat(result.getIdUser()).isEqualTo(999);
        verify(userFacade, never()).findUserByUsername(any());
    }

    @Test
    @DisplayName("updateLoan should throw access denied when the requesting user is not the owner and lacks WRITE_ALL")
    void updateLoan_ShouldThrowAccessDenied_WhenRequestingUserIsNotOwnerAndHasNoWriteAllPrivilege() {
        authenticateAs("john", "ROLE_FINANCE");
        Loan existingLoan = Loan.builder().id(1).idUser(7).build();
        when(loanService.findLoanById(1, false)).thenReturn(existingLoan);
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(99L).build());

        Loan loanToUpdate = Loan.builder().id(1).idUser(7).build();

        assertThatThrownBy(() -> loanFacade.updateLoan(loanToUpdate))
                .isInstanceOf(AccessDeniedException.class);

        verify(loanService, never()).updateLoan(any());
    }

    @Test
    @DisplayName("updateLoan should override the idUser back to the owner when the owner tries to reassign the loan to someone else")
    void updateLoan_ShouldOverrideRequestedIdUser_WhenOwnerTriesToReassignLoanToSomeoneElse() {
        authenticateAs("john", "ROLE_FINANCE");
        Loan existingLoan = Loan.builder().id(1).idUser(7).build();
        when(loanService.findLoanById(1, false)).thenReturn(existingLoan);
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(7L).build());
        // proba przepisania wlasnego kredytu na inna osobe (idUser=999)
        Loan loanToUpdate = Loan.builder().id(1).idUser(999).build();
        Loan updatedLoan = Loan.builder().id(1).idUser(7).build();
        when(loanService.findLoanById(1, true)).thenReturn(updatedLoan);

        Loan result = loanFacade.updateLoan(loanToUpdate);

        assertThat(loanToUpdate.getIdUser()).isEqualTo(7);
        assertThat(result.getIdUser()).isEqualTo(7);
        verify(loanService).updateLoan(loanToUpdate);
    }

    @Test
    @DisplayName("updateLoan should keep the requested idUser when the user has the WRITE_ALL authority")
    void updateLoan_ShouldKeepRequestedIdUser_WhenUserHasWriteAllAuthority() {
        authenticateAs("admin", "FINANCE_LOAN_WRITE_ALL");
        Loan existingLoan = Loan.builder().id(1).idUser(7).build();
        when(loanService.findLoanById(1, false)).thenReturn(existingLoan);
        Loan loanToUpdate = Loan.builder().id(1).idUser(999).build();
        Loan updatedLoan = Loan.builder().id(1).idUser(999).build();
        when(loanService.findLoanById(1, true)).thenReturn(updatedLoan);

        Loan result = loanFacade.updateLoan(loanToUpdate);

        assertThat(result.getIdUser()).isEqualTo(999);
        verify(userFacade, never()).findUserByUsername(any());
    }

    @Test
    @DisplayName("updateLoan should throw access denied when the user has only READ_ALL, since it does not allow editing others' loans")
    void updateLoan_ShouldThrowAccessDenied_WhenUserHasOnlyReadAllAuthority() {
        // sam READ_ALL (bez WRITE_ALL) nie uprawnia do edycji cudzego kredytu
        authenticateAs("john", "FINANCE_LOAN_READ_ALL");
        Loan existingLoan = Loan.builder().id(1).idUser(7).build();
        when(loanService.findLoanById(1, false)).thenReturn(existingLoan);
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(99L).build());

        Loan loanToUpdate = Loan.builder().id(1).idUser(7).build();

        assertThatThrownBy(() -> loanFacade.updateLoan(loanToUpdate))
                .isInstanceOf(AccessDeniedException.class);

        verify(loanService, never()).updateLoan(any());
    }

    @Test
    @DisplayName("updateLoanStatus should throw access denied when the requesting user is not the owner and lacks WRITE_ALL")
    void updateLoanStatus_ShouldThrowAccessDenied_WhenRequestingUserIsNotOwnerAndHasNoWriteAllPrivilege() {
        authenticateAs("john", "ROLE_FINANCE");
        Loan existingLoan = Loan.builder().id(1).idUser(7).build();
        when(loanService.findLoanById(1, false)).thenReturn(existingLoan);
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(99L).build());

        assertThatThrownBy(() -> loanFacade.updateLoanStatus(1, PaymentStatus.PAID))
                .isInstanceOf(AccessDeniedException.class);

        verify(loanService, never()).updateLoan(any());
    }

    @Test
    @DisplayName("updateLoanStatus should update the status when the requesting user is the loan's owner")
    void updateLoanStatus_ShouldUpdateStatus_WhenRequestingUserIsOwner() {
        authenticateAs("john", "ROLE_FINANCE");
        Loan existingLoan = Loan.builder().id(1).idUser(7).loanStatus(PaymentStatus.TO_PAY).build();
        when(loanService.findLoanById(1, false)).thenReturn(existingLoan);
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(7L).build());
        Loan updatedLoan = Loan.builder().id(1).idUser(7).loanStatus(PaymentStatus.PAID).build();
        when(loanService.findLoanById(1, true)).thenReturn(updatedLoan);

        Loan result = loanFacade.updateLoanStatus(1, PaymentStatus.PAID);

        assertThat(result.getLoanStatus()).isEqualTo(PaymentStatus.PAID);
        verify(loanService).updateLoan(existingLoan);
    }

    @Test
    @DisplayName("updateLoanStatus should update the status of another user's loan when the user has the WRITE_ALL authority")
    void updateLoanStatus_ShouldUpdateStatus_WhenUserHasWriteAllAuthority() {
        authenticateAs("admin", "FINANCE_LOAN_WRITE_ALL");
        Loan existingLoan = Loan.builder().id(1).idUser(7).loanStatus(PaymentStatus.TO_PAY).build();
        when(loanService.findLoanById(1, false)).thenReturn(existingLoan);
        Loan updatedLoan = Loan.builder().id(1).idUser(7).loanStatus(PaymentStatus.PAID).build();
        when(loanService.findLoanById(1, true)).thenReturn(updatedLoan);

        Loan result = loanFacade.updateLoanStatus(1, PaymentStatus.PAID);

        assertThat(result.getLoanStatus()).isEqualTo(PaymentStatus.PAID);
        verify(userFacade, never()).findUserByUsername(any());
    }

    @Test
    @DisplayName("updateLoanStatus should throw access denied when the user has only READ_ALL, since it does not allow changing others' loan status")
    void updateLoanStatus_ShouldThrowAccessDenied_WhenUserHasOnlyReadAllAuthority() {
        // sam READ_ALL (bez WRITE_ALL) nie uprawnia do zmiany statusu cudzego kredytu
        authenticateAs("john", "FINANCE_LOAN_READ_ALL");
        Loan existingLoan = Loan.builder().id(1).idUser(7).build();
        when(loanService.findLoanById(1, false)).thenReturn(existingLoan);
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(99L).build());

        assertThatThrownBy(() -> loanFacade.updateLoanStatus(1, PaymentStatus.PAID))
                .isInstanceOf(AccessDeniedException.class);

        verify(loanService, never()).updateLoan(any());
    }
}
