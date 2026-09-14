package net.focik.homeoffice.finance.domain.card;

import net.focik.homeoffice.userservice.domain.AppUser;
import net.focik.homeoffice.userservice.domain.UserFacade;
import net.focik.homeoffice.utils.share.ActiveStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
 * Kontrola dostepu w CardFacade (na wzor LoanFacadeTest/FeeFacadeTest/PurchaseFacadeTest) -
 * rozdzial FINANCE_READ_ALL / FINANCE_WRITE_ALL / FINANCE_DELETE_ALL i ochrona przed dzialaniem
 * na cudzych kartach bez odpowiedniego uprawnienia.
 */
@ExtendWith(MockitoExtension.class)
class CardFacadeTest {

    @Mock
    private CardService cardService;
    @Mock
    private UserFacade userFacade;

    private CardFacade cardFacade;

    @BeforeEach
    void setUp() {
        cardFacade = new CardFacade(cardService, userFacade);
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
    void findById_ShouldReturnCard_WhenNoAuthenticationContext() {
        Card card = Card.builder().id(1).idUser(5).build();
        when(cardService.findCardById(1)).thenReturn(card);

        Card result = cardFacade.findById(1);

        assertThat(result).isEqualTo(card);
        verify(userFacade, never()).findUserByUsername(any());
    }

    @Test
    void findById_ShouldReturnCard_WhenRequestingUserIsOwner() {
        authenticateAs("john", "ROLE_FINANCE");
        Card card = Card.builder().id(1).idUser(7).build();
        when(cardService.findCardById(1)).thenReturn(card);
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(7L).build());

        Card result = cardFacade.findById(1);

        assertThat(result).isEqualTo(card);
    }

    @Test
    void findById_ShouldThrowAccessDenied_WhenRequestingUserIsNotOwnerAndHasNoReadAllPrivilege() {
        authenticateAs("john", "FINANCE_READ");
        Card card = Card.builder().id(1).idUser(7).build();
        when(cardService.findCardById(1)).thenReturn(card);
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(99L).build());

        assertThatThrownBy(() -> cardFacade.findById(1))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void findById_ShouldReturnAnyCard_WhenUserHasReadAllAuthority() {
        authenticateAs("admin", "FINANCE_READ_ALL");
        Card card = Card.builder().id(1).idUser(7).build();
        when(cardService.findCardById(1)).thenReturn(card);

        Card result = cardFacade.findById(1);

        assertThat(result).isEqualTo(card);
        verify(userFacade, never()).findUserByUsername(any());
    }

    @Test
    void findById_ShouldThrowAccessDenied_WhenUserHasOnlyWriteAllAuthority() {
        authenticateAs("john", "FINANCE_WRITE_ALL");
        Card card = Card.builder().id(1).idUser(7).build();
        when(cardService.findCardById(1)).thenReturn(card);
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(99L).build());

        assertThatThrownBy(() -> cardFacade.findById(1))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ---- findByStatus (endpoint GET / - dziś dostepny dla kazdego ROLE_FINANCE) ----

    @Test
    void findByStatus_ShouldFilterToOwnCards_WhenUserHasNoReadAllPrivilege() {
        authenticateAs("john", "ROLE_FINANCE");
        Card own = Card.builder().id(1).idUser(7).build();
        Card someoneElses = Card.builder().id(2).idUser(99).build();
        when(cardService.findCardsByStatus(ActiveStatus.ALL)).thenReturn(List.of(own, someoneElses));
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(7L).build());

        List<Card> result = cardFacade.findByStatus(ActiveStatus.ALL);

        assertThat(result).containsExactly(own);
    }

    @Test
    void findByStatus_ShouldReturnEverything_WhenUserHasReadAllAuthority() {
        authenticateAs("admin", "ROLE_ADMIN");
        Card own = Card.builder().id(1).idUser(7).build();
        Card someoneElses = Card.builder().id(2).idUser(99).build();
        when(cardService.findCardsByStatus(ActiveStatus.ALL)).thenReturn(List.of(own, someoneElses));

        List<Card> result = cardFacade.findByStatus(ActiveStatus.ALL);

        assertThat(result).containsExactly(own, someoneElses);
        verify(userFacade, never()).findUserByUsername(any());
    }

    // ---- findByUserAndStatus (endpoint /user/{userId}) ----

    @Test
    void findByUserAndStatus_ShouldThrowAccessDenied_WhenRequestingOtherUsersDataWithoutReadAllPrivilege() {
        authenticateAs("john", "FINANCE_READ");
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(7L).build());

        assertThatThrownBy(() -> cardFacade.findByUserAndStatus(999, ActiveStatus.ALL))
                .isInstanceOf(AccessDeniedException.class);

        verify(cardService, never()).findCardsByUserAndStatus(anyInt(), any());
    }

    @Test
    void findByUserAndStatus_ShouldReturnAnyUsersData_WhenUserHasReadAllAuthority() {
        authenticateAs("admin", "ROLE_ADMIN");
        when(cardService.findCardsByUserAndStatus(999, ActiveStatus.ALL)).thenReturn(List.of());

        cardFacade.findByUserAndStatus(999, ActiveStatus.ALL);

        verify(userFacade, never()).findUserByUsername(any());
    }

    // ---- addCard (WRITE) ----

    @Test
    void addCard_ShouldOverrideRequestedIdUser_WhenUserHasNoWriteAllPrivilege() {
        authenticateAs("john", "FINANCE_WRITE");
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(7L).build());
        Card cardToAdd = Card.builder().idUser(999).build();
        when(cardService.addCard(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Card result = cardFacade.addCard(cardToAdd);

        assertThat(result.getIdUser()).isEqualTo(7);
    }

    @Test
    void addCard_ShouldKeepRequestedIdUser_WhenUserHasWriteAllAuthority() {
        authenticateAs("admin", "FINANCE_WRITE_ALL");
        Card cardToAdd = Card.builder().idUser(999).build();
        when(cardService.addCard(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Card result = cardFacade.addCard(cardToAdd);

        assertThat(result.getIdUser()).isEqualTo(999);
        verify(userFacade, never()).findUserByUsername(any());
    }

    // ---- updateCard / updateCardStatus (WRITE) ----

    @Test
    void updateCard_ShouldThrowAccessDenied_WhenRequestingUserIsNotOwnerAndHasNoWriteAllPrivilege() {
        authenticateAs("john", "FINANCE_WRITE");
        Card existingCard = Card.builder().id(1).idUser(7).build();
        when(cardService.findCardById(1)).thenReturn(existingCard);
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(99L).build());

        Card cardToUpdate = Card.builder().id(1).idUser(7).build();

        assertThatThrownBy(() -> cardFacade.updateCard(cardToUpdate))
                .isInstanceOf(AccessDeniedException.class);

        verify(cardService, never()).updateCard(any());
    }

    @Test
    void updateCard_ShouldOverrideRequestedIdUser_WhenOwnerTriesToReassignCardToSomeoneElse() {
        authenticateAs("john", "FINANCE_WRITE");
        Card existingCard = Card.builder().id(1).idUser(7).build();
        when(cardService.findCardById(1)).thenReturn(existingCard);
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(7L).build());
        Card cardToUpdate = Card.builder().id(1).idUser(999).build();
        when(cardService.updateCard(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Card result = cardFacade.updateCard(cardToUpdate);

        assertThat(result.getIdUser()).isEqualTo(7);
    }

    @Test
    void updateCardStatus_ShouldThrowAccessDenied_WhenRequestingUserIsNotOwnerAndHasNoWriteAllPrivilege() {
        authenticateAs("john", "FINANCE_WRITE");
        Card existingCard = Card.builder().id(1).idUser(7).build();
        when(cardService.findCardById(1)).thenReturn(existingCard);
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(99L).build());

        assertThatThrownBy(() -> cardFacade.updateCardStatus(1, ActiveStatus.INACTIVE))
                .isInstanceOf(AccessDeniedException.class);

        verify(cardService, never()).updateCard(any());
    }

    // ---- deleteCard (DELETE) ----

    @Test
    void deleteCard_ShouldThrowAccessDenied_WhenRequestingUserIsNotOwnerAndHasNoDeleteAllPrivilege() {
        authenticateAs("john", "FINANCE_DELETE");
        when(cardService.findCardById(99)).thenReturn(Card.builder().id(99).idUser(7).build());
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(99L).build());

        assertThatThrownBy(() -> cardFacade.deleteCard(99))
                .isInstanceOf(AccessDeniedException.class);

        verify(cardService, never()).deleteCard(anyInt());
    }

    @Test
    void deleteCard_ShouldThrowAccessDenied_WhenUserHasOnlyWriteAllAuthority() {
        authenticateAs("john", "FINANCE_WRITE_ALL");
        when(cardService.findCardById(99)).thenReturn(Card.builder().id(99).idUser(7).build());
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(99L).build());

        assertThatThrownBy(() -> cardFacade.deleteCard(99))
                .isInstanceOf(AccessDeniedException.class);

        verify(cardService, never()).deleteCard(anyInt());
    }

    @Test
    void deleteCard_ShouldDeleteOwnCard_WhenRequestingUserIsOwner() {
        authenticateAs("john", "FINANCE_DELETE");
        when(cardService.findCardById(99)).thenReturn(Card.builder().id(99).idUser(7).build());
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(7L).build());

        cardFacade.deleteCard(99);

        verify(cardService).deleteCard(99);
    }

    @Test
    void deleteCard_ShouldDeleteAnyCard_WhenUserHasDeleteAllAuthority() {
        authenticateAs("admin", "FINANCE_DELETE_ALL");
        when(cardService.findCardById(99)).thenReturn(Card.builder().id(99).idUser(7).build());

        cardFacade.deleteCard(99);

        verify(cardService).deleteCard(99);
        verify(userFacade, never()).findUserByUsername(any());
    }
}
