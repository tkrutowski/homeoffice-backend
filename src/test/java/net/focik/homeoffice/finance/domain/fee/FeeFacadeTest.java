package net.focik.homeoffice.finance.domain.fee;

import net.focik.homeoffice.finance.api.mapper.ApiFeeMapper;
import net.focik.homeoffice.finance.infrastructure.jpa.BankTransactionDtoRepository;
import net.focik.homeoffice.userservice.domain.AppUser;
import net.focik.homeoffice.userservice.domain.UserFacade;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Kontrola dostepu w FeeFacade (na wzor LoanFacadeTest) - rozdzial READ_ALL / WRITE_ALL / DELETE_ALL
 * i ochrona przed dzialaniem na cudzych oplatach bez odpowiedniego uprawnienia.
 */
@ExtendWith(MockitoExtension.class)
class FeeFacadeTest {

    @Mock
    private FeeService feeService;
    @Mock
    private UserFacade userFacade;
    @Mock
    private BankTransactionDtoRepository bankTransactionRepository;
    @Mock
    private ApiFeeMapper apiFeeMapper;

    private FeeFacade feeFacade;

    @BeforeEach
    void setUp() {
        feeFacade = new FeeFacade(feeService, userFacade, bankTransactionRepository, apiFeeMapper);
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

    // ---- getFeeById (READ) ----

    @Test
    void getFeeById_ShouldReturnFee_WhenNoAuthenticationContext() {
        Fee fee = Fee.builder().id(1).idUser(5).build();
        when(feeService.findFeeById(1, true)).thenReturn(fee);

        Fee result = feeFacade.getFeeById(1, true);

        assertThat(result).isEqualTo(fee);
        verify(userFacade, never()).findUserByUsername(any());
    }

    @Test
    void getFeeById_ShouldReturnFee_WhenRequestingUserIsOwner() {
        authenticateAs("john", "ROLE_FINANCE");
        Fee fee = Fee.builder().id(1).idUser(7).build();
        when(feeService.findFeeById(1, true)).thenReturn(fee);
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(7L).build());

        Fee result = feeFacade.getFeeById(1, true);

        assertThat(result).isEqualTo(fee);
    }

    @Test
    void getFeeById_ShouldThrowAccessDenied_WhenRequestingUserIsNotOwnerAndHasNoReadAllPrivilege() {
        authenticateAs("john", "ROLE_FINANCE");
        Fee fee = Fee.builder().id(1).idUser(7).build();
        when(feeService.findFeeById(1, true)).thenReturn(fee);
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(99L).build());

        assertThatThrownBy(() -> feeFacade.getFeeById(1, true))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getFeeById_ShouldReturnAnyFee_WhenUserHasReadAllAuthority() {
        authenticateAs("admin", "FINANCE_FEE_READ_ALL");
        Fee fee = Fee.builder().id(1).idUser(7).build();
        when(feeService.findFeeById(1, true)).thenReturn(fee);

        Fee result = feeFacade.getFeeById(1, true);

        assertThat(result).isEqualTo(fee);
        verify(userFacade, never()).findUserByUsername(any());
    }

    @Test
    void getFeeById_ShouldThrowAccessDenied_WhenUserHasOnlyWriteAllAuthority() {
        // READ_ALL i WRITE_ALL to celowo osobne uprawnienia
        authenticateAs("john", "FINANCE_FEE_WRITE_ALL");
        Fee fee = Fee.builder().id(1).idUser(7).build();
        when(feeService.findFeeById(1, true)).thenReturn(fee);
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(99L).build());

        assertThatThrownBy(() -> feeFacade.getFeeById(1, true))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ---- getFeesByUser (endpoint /{idUser}/status) ----

    @Test
    void getFeesByUser_ShouldThrowAccessDenied_WhenRequestingOtherUsersDataWithoutReadAllPrivilege() {
        authenticateAs("john", "ROLE_FINANCE");
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(7L).build());

        assertThatThrownBy(() -> feeFacade.getFeesByUser(999, PaymentStatus.TO_PAY, true))
                .isInstanceOf(AccessDeniedException.class);

        verify(feeService, never()).findFeesByUser(anyInt(), any(), anyBoolean());
    }

    @Test
    void getFeesByUser_ShouldReturnOwnData_WhenRequestingOwnId() {
        authenticateAs("john", "ROLE_FINANCE");
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(7L).build());
        when(feeService.findFeesByUser(7, PaymentStatus.TO_PAY, true)).thenReturn(List.of());

        List<Fee> result = feeFacade.getFeesByUser(7, PaymentStatus.TO_PAY, true);

        assertThat(result).isEmpty();
    }

    @Test
    void getFeesByUser_ShouldReturnAnyUsersData_WhenUserHasReadAllAuthority() {
        authenticateAs("admin", "ROLE_ADMIN");
        when(feeService.findFeesByUser(999, PaymentStatus.TO_PAY, true)).thenReturn(List.of());

        List<Fee> result = feeFacade.getFeesByUser(999, PaymentStatus.TO_PAY, true);

        assertThat(result).isEmpty();
        verify(userFacade, never()).findUserByUsername(any());
    }

    // ---- findFeesPageableWithFilters (/page) ----

    @Test
    void findFeesPageableWithFilters_ShouldOverrideRequestedIdUser_WhenUserHasNoReadAllPrivilege() {
        authenticateAs("john", "ROLE_FINANCE");
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(7L).build());
        when(feeService.findFeesPageableWithFilters(
                eq(0), eq(20), eq("date"), eq("DESC"), isNull(), isNull(), isNull(),
                isNull(), eq("EQUALS"), isNull(), eq("EQUALS"), isNull(), eq(7)))
                .thenReturn(Page.empty());

        feeFacade.findFeesPageableWithFilters(0, 20, "date", "DESC", null, null, null,
                null, "EQUALS", null, "EQUALS", null, 999);

        verify(feeService).findFeesPageableWithFilters(
                eq(0), eq(20), eq("date"), eq("DESC"), isNull(), isNull(), isNull(),
                isNull(), eq("EQUALS"), isNull(), eq("EQUALS"), isNull(), eq(7));
    }

    // ---- addFee (WRITE) ----

    @Test
    void addFee_ShouldOverrideRequestedIdUser_WhenUserHasNoWriteAllPrivilege() {
        authenticateAs("john", "ROLE_FINANCE");
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(7L).build());
        Fee feeToAdd = Fee.builder().idUser(999).build();
        when(feeService.saveFee(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Fee result = feeFacade.addFee(feeToAdd);

        assertThat(result.getIdUser()).isEqualTo(7);
    }

    @Test
    void addFee_ShouldKeepRequestedIdUser_WhenUserHasWriteAllAuthority() {
        authenticateAs("admin", "FINANCE_FEE_WRITE_ALL");
        Fee feeToAdd = Fee.builder().idUser(999).build();
        when(feeService.saveFee(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Fee result = feeFacade.addFee(feeToAdd);

        assertThat(result.getIdUser()).isEqualTo(999);
        verify(userFacade, never()).findUserByUsername(any());
    }

    // ---- updateFee / updateFeeStatus (WRITE) ----

    @Test
    void updateFee_ShouldThrowAccessDenied_WhenRequestingUserIsNotOwnerAndHasNoWriteAllPrivilege() {
        authenticateAs("john", "ROLE_FINANCE");
        Fee existingFee = Fee.builder().id(1).idUser(7).build();
        when(feeService.findFeeById(1, false)).thenReturn(existingFee);
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(99L).build());

        Fee feeToUpdate = Fee.builder().id(1).idUser(7).build();

        assertThatThrownBy(() -> feeFacade.updateFee(feeToUpdate))
                .isInstanceOf(AccessDeniedException.class);

        verify(feeService, never()).updateFee(any());
    }

    @Test
    void updateFee_ShouldOverrideRequestedIdUser_WhenOwnerTriesToReassignFeeToSomeoneElse() {
        authenticateAs("john", "ROLE_FINANCE");
        Fee existingFee = Fee.builder().id(1).idUser(7).build();
        when(feeService.findFeeById(1, false)).thenReturn(existingFee);
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(7L).build());
        Fee feeToUpdate = Fee.builder().id(1).idUser(999).build();
        Fee updatedFee = Fee.builder().id(1).idUser(7).build();
        when(feeService.findFeeById(1, true)).thenReturn(updatedFee);

        Fee result = feeFacade.updateFee(feeToUpdate);

        assertThat(feeToUpdate.getIdUser()).isEqualTo(7);
        assertThat(result.getIdUser()).isEqualTo(7);
        verify(feeService).updateFee(feeToUpdate);
    }

    @Test
    void updateFeeStatus_ShouldThrowAccessDenied_WhenRequestingUserIsNotOwnerAndHasNoWriteAllPrivilege() {
        authenticateAs("john", "ROLE_FINANCE");
        Fee existingFee = Fee.builder().id(1).idUser(7).build();
        when(feeService.findFeeById(1, false)).thenReturn(existingFee);
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(99L).build());

        assertThatThrownBy(() -> feeFacade.updateFeeStatus(1, PaymentStatus.PAID))
                .isInstanceOf(AccessDeniedException.class);

        verify(feeService, never()).updateFee(any());
    }

    @Test
    void updateFeeStatus_ShouldUpdateStatus_WhenUserHasWriteAllAuthority() {
        authenticateAs("admin", "FINANCE_FEE_WRITE_ALL");
        Fee existingFee = Fee.builder().id(1).idUser(7).feeStatus(PaymentStatus.TO_PAY).build();
        when(feeService.findFeeById(1, false)).thenReturn(existingFee);
        Fee updatedFee = Fee.builder().id(1).idUser(7).feeStatus(PaymentStatus.PAID).build();
        when(feeService.findFeeById(1, true)).thenReturn(updatedFee);

        Fee result = feeFacade.updateFeeStatus(1, PaymentStatus.PAID);

        assertThat(result.getFeeStatus()).isEqualTo(PaymentStatus.PAID);
        verify(userFacade, never()).findUserByUsername(any());
    }

    // ---- deleteFeeById / deleteFeeInstallmentById (DELETE) ----

    @Test
    void deleteFeeById_ShouldThrowAccessDenied_WhenRequestingUserIsNotOwnerAndHasNoDeleteAllPrivilege() {
        authenticateAs("john", "ROLE_FINANCE");
        when(feeService.findFeeById(99, false)).thenReturn(Fee.builder().id(99).idUser(7).build());
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(99L).build());

        assertThatThrownBy(() -> feeFacade.deleteFeeById(99))
                .isInstanceOf(AccessDeniedException.class);

        verify(feeService, never()).deleteFee(anyInt());
    }

    @Test
    void deleteFeeById_ShouldThrowAccessDenied_WhenUserHasOnlyWriteAllAuthority() {
        authenticateAs("john", "FINANCE_FEE_WRITE_ALL");
        when(feeService.findFeeById(99, false)).thenReturn(Fee.builder().id(99).idUser(7).build());
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(99L).build());

        assertThatThrownBy(() -> feeFacade.deleteFeeById(99))
                .isInstanceOf(AccessDeniedException.class);

        verify(feeService, never()).deleteFee(anyInt());
    }

    @Test
    void deleteFeeById_ShouldDeleteOwnFee_WhenRequestingUserIsOwner() {
        authenticateAs("john", "ROLE_FINANCE");
        when(feeService.findFeeById(99, false)).thenReturn(Fee.builder().id(99).idUser(7).build());
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(7L).build());

        feeFacade.deleteFeeById(99);

        verify(feeService).deleteFee(99);
    }

    @Test
    void deleteFeeById_ShouldDeleteAnyFee_WhenUserHasDeleteAllAuthority() {
        authenticateAs("admin", "FINANCE_FEE_DELETE_ALL");
        when(feeService.findFeeById(99, false)).thenReturn(Fee.builder().id(99).idUser(7).build());

        feeFacade.deleteFeeById(99);

        verify(feeService).deleteFee(99);
        verify(userFacade, never()).findUserByUsername(any());
    }

    @Test
    void deleteFeeInstallmentById_ShouldThrowAccessDenied_WhenRequestingUserIsNotOwnerAndHasNoDeleteAllPrivilege() {
        authenticateAs("john", "ROLE_FINANCE");
        FeeInstallment installment = FeeInstallment.builder().idFeeInstallment(5).idFee(99).build();
        when(feeService.getFeeInstallment(5)).thenReturn(installment);
        when(feeService.findFeeById(99, false)).thenReturn(Fee.builder().id(99).idUser(7).build());
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(99L).build());

        assertThatThrownBy(() -> feeFacade.deleteFeeInstallmentById(5))
                .isInstanceOf(AccessDeniedException.class);

        verify(feeService, never()).deleteFeeInstallment(anyInt());
    }

    @Test
    void deleteFeeInstallmentById_ShouldDelete_WhenUserHasDeleteAllAuthority() {
        authenticateAs("admin", "FINANCE_FEE_DELETE_ALL");
        FeeInstallment installment = FeeInstallment.builder().idFeeInstallment(5).idFee(99).build();
        when(feeService.getFeeInstallment(5)).thenReturn(installment);
        when(feeService.findFeeById(99, false)).thenReturn(Fee.builder().id(99).idUser(7).build());

        feeFacade.deleteFeeInstallmentById(5);

        verify(feeService).deleteFeeInstallment(5);
        verify(userFacade, never()).findUserByUsername(any());
    }
}
