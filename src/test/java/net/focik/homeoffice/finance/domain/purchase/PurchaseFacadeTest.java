package net.focik.homeoffice.finance.domain.purchase;

import net.focik.homeoffice.finance.api.mapper.ApiPurchaseMapper;
import net.focik.homeoffice.finance.domain.card.CardFacade;
import net.focik.homeoffice.finance.infrastructure.jpa.BankTransactionDtoRepository;
import net.focik.homeoffice.userservice.domain.AppUser;
import net.focik.homeoffice.userservice.domain.UserFacade;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Kontrola dostepu w PurchaseFacade (na wzor LoanFacadeTest/FeeFacadeTest) - rozdzial
 * READ_ALL / WRITE_ALL / DELETE_ALL i ochrona przed dzialaniem na cudzych zakupach bez
 * odpowiedniego uprawnienia.
 */
@ExtendWith(MockitoExtension.class)
class PurchaseFacadeTest {

    @Mock
    private PurchaseService purchaseService;
    @Mock
    private UserFacade userFacade;
    @Mock
    private BankTransactionDtoRepository bankTransactionRepository;
    @Mock
    private ApiPurchaseMapper apiPurchaseMapper;
    @Mock
    private CardFacade cardFacade;

    private PurchaseFacade purchaseFacade;

    @BeforeEach
    void setUp() {
        purchaseFacade = new PurchaseFacade(purchaseService, userFacade, bankTransactionRepository, apiPurchaseMapper, cardFacade);
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

    // ---- findById (READ) ----

    @Test
    @DisplayName("findById should return the purchase without an ownership check when there is no authentication context")
    void findById_ShouldReturnPurchase_WhenNoAuthenticationContext() {
        Purchase purchase = Purchase.builder().id(1).idUser(5).build();
        when(purchaseService.findPurchaseById(1)).thenReturn(purchase);

        Purchase result = purchaseFacade.findById(1);

        assertThat(result).isEqualTo(purchase);
        verify(userFacade, never()).findUserByUsername(any());
    }

    @Test
    @DisplayName("findById should return the purchase when the requesting user is its owner")
    void findById_ShouldReturnPurchase_WhenRequestingUserIsOwner() {
        authenticateAs("john", "ROLE_FINANCE");
        Purchase purchase = Purchase.builder().id(1).idUser(7).build();
        when(purchaseService.findPurchaseById(1)).thenReturn(purchase);
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(7L).build());

        Purchase result = purchaseFacade.findById(1);

        assertThat(result).isEqualTo(purchase);
    }

    @Test
    @DisplayName("findById should throw access denied when the requesting user is not the owner and lacks READ_ALL")
    void findById_ShouldThrowAccessDenied_WhenRequestingUserIsNotOwnerAndHasNoReadAllPrivilege() {
        authenticateAs("john", "ROLE_FINANCE");
        Purchase purchase = Purchase.builder().id(1).idUser(7).build();
        when(purchaseService.findPurchaseById(1)).thenReturn(purchase);
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(99L).build());

        assertThatThrownBy(() -> purchaseFacade.findById(1))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("findById should return any purchase, even someone else's, when the user has the READ_ALL authority")
    void findById_ShouldReturnAnyPurchase_WhenUserHasReadAllAuthority() {
        authenticateAs("admin", "FINANCE_PURCHASE_READ_ALL");
        Purchase purchase = Purchase.builder().id(1).idUser(7).build();
        when(purchaseService.findPurchaseById(1)).thenReturn(purchase);

        Purchase result = purchaseFacade.findById(1);

        assertThat(result).isEqualTo(purchase);
        verify(userFacade, never()).findUserByUsername(any());
    }

    @Test
    @DisplayName("findById should throw access denied when the user has only the WRITE_ALL authority")
    void findById_ShouldThrowAccessDenied_WhenUserHasOnlyWriteAllAuthority() {
        authenticateAs("john", "FINANCE_PURCHASE_WRITE_ALL");
        Purchase purchase = Purchase.builder().id(1).idUser(7).build();
        when(purchaseService.findPurchaseById(1)).thenReturn(purchase);
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(99L).build());

        assertThatThrownBy(() -> purchaseFacade.findById(1))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ---- findByUserMap(Integer userId) (endpoint /user/{userId}) ----

    @Test
    @DisplayName("findByUserMap should throw access denied when requesting another user's purchases without READ_ALL")
    void findByUserMapById_ShouldThrowAccessDenied_WhenRequestingOtherUsersDataWithoutReadAllPrivilege() {
        authenticateAs("john", "ROLE_FINANCE");
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(7L).build());

        assertThatThrownBy(() -> purchaseFacade.findByUserMap(999, PaymentStatus.TO_PAY, null))
                .isInstanceOf(AccessDeniedException.class);

        verify(purchaseService, never()).findPurchasesByUser(anyInt(), any());
    }

    @Test
    @DisplayName("findByUserMap should return another user's purchases when the requester has ROLE_ADMIN")
    void findByUserMapById_ShouldReturnAnyUsersData_WhenUserHasReadAllAuthority() {
        authenticateAs("admin", "ROLE_ADMIN");
        when(purchaseService.findPurchasesByUser(999, PaymentStatus.TO_PAY)).thenReturn(List.of());
        when(purchaseService.convertToMapByDeadline(List.of())).thenReturn(java.util.Map.of());

        purchaseFacade.findByUserMap(999, PaymentStatus.TO_PAY, null);

        verify(userFacade, never()).findUserByUsername(any());
    }

    // ---- findCurrent(username) (endpoint /current/{username}) ----

    @Test
    @DisplayName("findCurrent should throw access denied when requesting another user's current purchases without READ_ALL")
    void findCurrent_ShouldThrowAccessDenied_WhenRequestingOtherUsersUsernameWithoutReadAllPrivilege() {
        authenticateAs("john", "ROLE_FINANCE");

        assertThatThrownBy(() -> purchaseFacade.findCurrent("someone-else"))
                .isInstanceOf(AccessDeniedException.class);

        verify(purchaseService, never()).findCurrent(anyInt());
    }

    @Test
    @DisplayName("findCurrent should return the caller's own data when requesting their own username")
    void findCurrent_ShouldReturnOwnData_WhenRequestingOwnUsername() {
        authenticateAs("john", "ROLE_FINANCE");
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(7L).build());
        when(purchaseService.findCurrent(7)).thenReturn(List.of());
        when(purchaseService.convertToMapByDeadline(List.of())).thenReturn(java.util.Map.of());

        purchaseFacade.findCurrent("john");

        verify(purchaseService).findCurrent(7);
    }

    // ---- addPurchase (WRITE) ----

    @Test
    @DisplayName("addPurchase should override the requested idUser with the caller's own id when they lack WRITE_ALL")
    void addPurchase_ShouldOverrideRequestedIdUser_WhenUserHasNoWriteAllPrivilege() {
        authenticateAs("john", "ROLE_FINANCE");
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(7L).build());
        Purchase purchaseToAdd = Purchase.builder().idUser(999).build();
        when(purchaseService.addPurchase(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Purchase result = purchaseFacade.addPurchase(purchaseToAdd);

        assertThat(result.getIdUser()).isEqualTo(7);
    }

    @Test
    @DisplayName("addPurchase should keep the requested idUser when the user has the WRITE_ALL authority")
    void addPurchase_ShouldKeepRequestedIdUser_WhenUserHasWriteAllAuthority() {
        authenticateAs("admin", "FINANCE_PURCHASE_WRITE_ALL");
        Purchase purchaseToAdd = Purchase.builder().idUser(999).build();
        when(purchaseService.addPurchase(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Purchase result = purchaseFacade.addPurchase(purchaseToAdd);

        assertThat(result.getIdUser()).isEqualTo(999);
        verify(userFacade, never()).findUserByUsername(any());
    }

    // ---- updatePurchase / updatePurchaseStatus (WRITE) ----

    @Test
    @DisplayName("updatePurchase should throw access denied when the requesting user is not the owner and lacks WRITE_ALL")
    void updatePurchase_ShouldThrowAccessDenied_WhenRequestingUserIsNotOwnerAndHasNoWriteAllPrivilege() {
        authenticateAs("john", "ROLE_FINANCE");
        Purchase existingPurchase = Purchase.builder().id(1).idUser(7).build();
        when(purchaseService.findPurchaseById(1)).thenReturn(existingPurchase);
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(99L).build());

        Purchase purchaseToUpdate = Purchase.builder().id(1).idUser(7).build();

        assertThatThrownBy(() -> purchaseFacade.updatePurchase(purchaseToUpdate))
                .isInstanceOf(AccessDeniedException.class);

        verify(purchaseService, never()).updatePurchase(any());
    }

    @Test
    @DisplayName("updatePurchase should override the idUser back to the owner when the owner tries to reassign the purchase to someone else")
    void updatePurchase_ShouldOverrideRequestedIdUser_WhenOwnerTriesToReassignPurchaseToSomeoneElse() {
        authenticateAs("john", "ROLE_FINANCE");
        Purchase existingPurchase = Purchase.builder().id(1).idUser(7).build();
        when(purchaseService.findPurchaseById(1)).thenReturn(existingPurchase);
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(7L).build());
        Purchase purchaseToUpdate = Purchase.builder().id(1).idUser(999).build();
        when(purchaseService.updatePurchase(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Purchase result = purchaseFacade.updatePurchase(purchaseToUpdate);

        assertThat(result.getIdUser()).isEqualTo(7);
    }

    @Test
    @DisplayName("updatePurchaseStatus should throw access denied when the requesting user is not the owner and lacks WRITE_ALL")
    void updatePurchaseStatus_ShouldThrowAccessDenied_WhenRequestingUserIsNotOwnerAndHasNoWriteAllPrivilege() {
        authenticateAs("john", "ROLE_FINANCE");
        Purchase existingPurchase = Purchase.builder().id(1).idUser(7).paymentStatus(PaymentStatus.TO_PAY).build();
        when(purchaseService.findPurchaseById(1)).thenReturn(existingPurchase);
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(99L).build());

        assertThatThrownBy(() -> purchaseFacade.updatePurchaseStatus(1, PaymentStatus.TO_PAY))
                .isInstanceOf(AccessDeniedException.class);

        verify(purchaseService, never()).updatePurchase(any());
    }

    // ---- deletePurchase (DELETE) ----

    @Test
    @DisplayName("deletePurchase should throw access denied when the requesting user is not the owner and lacks DELETE_ALL")
    void deletePurchase_ShouldThrowAccessDenied_WhenRequestingUserIsNotOwnerAndHasNoDeleteAllPrivilege() {
        authenticateAs("john", "ROLE_FINANCE");
        when(purchaseService.findPurchaseById(99)).thenReturn(Purchase.builder().id(99).idUser(7).build());
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(99L).build());

        assertThatThrownBy(() -> purchaseFacade.deletePurchase(99))
                .isInstanceOf(AccessDeniedException.class);

        verify(purchaseService, never()).deletePurchase(anyInt());
    }

    @Test
    @DisplayName("deletePurchase should throw access denied when the user has only the WRITE_ALL authority, not DELETE_ALL")
    void deletePurchase_ShouldThrowAccessDenied_WhenUserHasOnlyWriteAllAuthority() {
        authenticateAs("john", "FINANCE_PURCHASE_WRITE_ALL");
        when(purchaseService.findPurchaseById(99)).thenReturn(Purchase.builder().id(99).idUser(7).build());
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(99L).build());

        assertThatThrownBy(() -> purchaseFacade.deletePurchase(99))
                .isInstanceOf(AccessDeniedException.class);

        verify(purchaseService, never()).deletePurchase(anyInt());
    }

    @Test
    @DisplayName("deletePurchase should delete the purchase when the requesting user is its owner")
    void deletePurchase_ShouldDeleteOwnPurchase_WhenRequestingUserIsOwner() {
        authenticateAs("john", "ROLE_FINANCE");
        when(purchaseService.findPurchaseById(99)).thenReturn(Purchase.builder().id(99).idUser(7).build());
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(7L).build());

        purchaseFacade.deletePurchase(99);

        verify(purchaseService).deletePurchase(99);
    }

    @Test
    @DisplayName("deletePurchase should delete any purchase, even someone else's, when the user has the DELETE_ALL authority")
    void deletePurchase_ShouldDeleteAnyPurchase_WhenUserHasDeleteAllAuthority() {
        authenticateAs("admin", "FINANCE_PURCHASE_DELETE_ALL");
        when(purchaseService.findPurchaseById(99)).thenReturn(Purchase.builder().id(99).idUser(7).build());

        purchaseFacade.deletePurchase(99);

        verify(purchaseService).deletePurchase(99);
        verify(userFacade, never()).findUserByUsername(any());
    }
}
