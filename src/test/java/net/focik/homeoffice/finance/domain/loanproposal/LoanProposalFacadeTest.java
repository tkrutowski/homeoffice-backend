package net.focik.homeoffice.finance.domain.loanproposal;

import net.focik.homeoffice.finance.domain.loan.Loan;
import net.focik.homeoffice.finance.domain.loan.port.primary.AddLoanUseCase;
import net.focik.homeoffice.finance.domain.loanproposal.port.secondary.LoanEmailArchivePort;
import net.focik.homeoffice.finance.domain.loanproposal.port.secondary.LoanProposalRepository;
import net.focik.homeoffice.finance.domain.purchase.port.primary.AddPurchaseUseCase;
import net.focik.homeoffice.userservice.domain.AppUser;
import net.focik.homeoffice.userservice.domain.UserFacade;
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
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Kontrola dostepu w LoanProposalFacade - propozycja jest widoczna/obslugiwalna tylko przez
 * przypisanego uzytkownika (idUser, dopasowany po adresie e-mail przy ingest) albo przez kogos
 * uprzywilejowanego (READ_ALL/WRITE_ALL Loan lub Purchase, ROLE_ADMIN).
 */
@ExtendWith(MockitoExtension.class)
class LoanProposalFacadeTest {

    @Mock
    private LoanProposalRepository loanProposalRepository;
    @Mock
    private LoanEmailArchivePort loanEmailArchivePort;
    @Mock
    private LoanProposalExtractionRunner extractionRunner;
    @Mock
    private AddLoanUseCase addLoanUseCase;
    @Mock
    private AddPurchaseUseCase addPurchaseUseCase;
    @Mock
    private UserFacade userFacade;

    private LoanProposalFacade facade;

    @BeforeEach
    void setUp() {
        facade = new LoanProposalFacade(loanProposalRepository, loanEmailArchivePort, extractionRunner,
                addLoanUseCase, addPurchaseUseCase, userFacade);
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

    private LoanProposal proposal(int id, Integer idUser, LoanProposalStatus status) {
        return LoanProposal.builder().id(id).idUser(idUser).status(status).build();
    }

    // ---- ingest: dopasowanie idUser po adresie nadawcy ----

    @Test
    void ingest_ShouldSetIdUser_WhenSenderEmailMatchesExistingUser() {
        when(loanProposalRepository.findBySourceMessageId("msg-1")).thenReturn(Optional.empty());
        when(loanEmailArchivePort.store(any(), any())).thenReturn(Optional.empty());
        when(userFacade.findUserByEmail("tkrutowski@gmail.com")).thenReturn(AppUser.builder().id(7L).build());
        when(loanProposalRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RawLoanEmail email = RawLoanEmail.builder()
                .messageId("msg-1").from("tkrutowski@gmail.com").subject("Allegro Pay").textBody("tresc").build();

        LoanProposal result = facade.ingest(email);

        assertThat(result.getIdUser()).isEqualTo(7);
    }

    @Test
    void ingest_ShouldExtractEmailFromAngleBracketsFormat() {
        when(loanProposalRepository.findBySourceMessageId("msg-2")).thenReturn(Optional.empty());
        when(loanEmailArchivePort.store(any(), any())).thenReturn(Optional.empty());
        when(userFacade.findUserByEmail("tkrutowski@gmail.com")).thenReturn(AppUser.builder().id(7L).build());
        when(loanProposalRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RawLoanEmail email = RawLoanEmail.builder()
                .messageId("msg-2").from("Tomek Krutowski <tkrutowski@gmail.com>").subject("x").textBody("y").build();

        LoanProposal result = facade.ingest(email);

        assertThat(result.getIdUser()).isEqualTo(7);
    }

    @Test
    void ingest_ShouldLeaveIdUserNull_WhenSenderDoesNotMatchAnyUser() {
        when(loanProposalRepository.findBySourceMessageId("msg-3")).thenReturn(Optional.empty());
        when(loanEmailArchivePort.store(any(), any())).thenReturn(Optional.empty());
        when(userFacade.findUserByEmail("unknown@example.com")).thenReturn(null);
        when(loanProposalRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RawLoanEmail email = RawLoanEmail.builder()
                .messageId("msg-3").from("unknown@example.com").subject("x").textBody("y").build();

        LoanProposal result = facade.ingest(email);

        assertThat(result.getIdUser()).isNull();
    }

    // ---- getLoanProposalById ----

    @Test
    void getLoanProposalById_ShouldReturnProposal_WhenNoAuthenticationContext() {
        LoanProposal proposal = proposal(1, 7, LoanProposalStatus.EXTRACTED);
        when(loanProposalRepository.findById(1)).thenReturn(Optional.of(proposal));

        LoanProposal result = facade.getLoanProposalById(1);

        assertThat(result).isEqualTo(proposal);
        verify(userFacade, never()).findUserByUsername(any());
    }

    @Test
    void getLoanProposalById_ShouldReturnProposal_WhenRequestingUserIsAssignedOwner() {
        authenticateAs("john", "ROLE_FINANCE");
        LoanProposal proposal = proposal(1, 7, LoanProposalStatus.EXTRACTED);
        when(loanProposalRepository.findById(1)).thenReturn(Optional.of(proposal));
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(7L).build());

        LoanProposal result = facade.getLoanProposalById(1);

        assertThat(result).isEqualTo(proposal);
    }

    @Test
    void getLoanProposalById_ShouldThrowAccessDenied_WhenRequestingUserIsNotAssignedOwner() {
        authenticateAs("john", "ROLE_FINANCE");
        LoanProposal proposal = proposal(1, 7, LoanProposalStatus.EXTRACTED);
        when(loanProposalRepository.findById(1)).thenReturn(Optional.of(proposal));
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(99L).build());

        assertThatThrownBy(() -> facade.getLoanProposalById(1))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getLoanProposalById_ShouldThrowAccessDenied_WhenProposalIsUnassignedAndUserHasNoPrivilege() {
        authenticateAs("john", "ROLE_FINANCE");
        LoanProposal proposal = proposal(1, null, LoanProposalStatus.EXTRACTED);
        when(loanProposalRepository.findById(1)).thenReturn(Optional.of(proposal));

        // idUser == null krotko-obwodowo pomija sprawdzenie wlasciciela - findUserByUsername
        // nigdy nie jest wolane, wiec celowo nie stubujemy go tutaj.
        assertThatThrownBy(() -> facade.getLoanProposalById(1))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getLoanProposalById_ShouldReturnUnassignedProposal_WhenUserHasReadAllAuthority() {
        authenticateAs("admin", "FINANCE_LOAN_READ_ALL");
        LoanProposal proposal = proposal(1, null, LoanProposalStatus.EXTRACTED);
        when(loanProposalRepository.findById(1)).thenReturn(Optional.of(proposal));

        LoanProposal result = facade.getLoanProposalById(1);

        assertThat(result).isEqualTo(proposal);
        verify(userFacade, never()).findUserByUsername(any());
    }

    @Test
    void getLoanProposalById_ShouldReturnAnyProposal_WhenUserHasPurchaseWriteAllAuthority() {
        authenticateAs("admin", "FINANCE_PURCHASE_WRITE_ALL");
        LoanProposal proposal = proposal(1, 7, LoanProposalStatus.EXTRACTED);
        when(loanProposalRepository.findById(1)).thenReturn(Optional.of(proposal));

        LoanProposal result = facade.getLoanProposalById(1);

        assertThat(result).isEqualTo(proposal);
    }

    // ---- getLoanProposalsByStatus (filtrowanie listy) ----

    @Test
    void getLoanProposalsByStatus_ShouldFilterToOwnAssignedProposals_WhenUserHasNoPrivilege() {
        authenticateAs("john", "ROLE_FINANCE");
        LoanProposal own = proposal(1, 7, LoanProposalStatus.EXTRACTED);
        LoanProposal someoneElses = proposal(2, 99, LoanProposalStatus.EXTRACTED);
        LoanProposal unassigned = proposal(3, null, LoanProposalStatus.EXTRACTED);
        when(loanProposalRepository.findByStatus(LoanProposalStatus.EXTRACTED))
                .thenReturn(List.of(own, someoneElses, unassigned));
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(7L).build());

        List<LoanProposal> result = facade.getLoanProposalsByStatus(LoanProposalStatus.EXTRACTED);

        assertThat(result).containsExactly(own);
    }

    @Test
    void getLoanProposalsByStatus_ShouldReturnEverything_WhenUserHasReadAllAuthority() {
        authenticateAs("admin", "ROLE_ADMIN");
        LoanProposal own = proposal(1, 7, LoanProposalStatus.EXTRACTED);
        LoanProposal someoneElses = proposal(2, 99, LoanProposalStatus.EXTRACTED);
        LoanProposal unassigned = proposal(3, null, LoanProposalStatus.EXTRACTED);
        when(loanProposalRepository.findByStatus(LoanProposalStatus.EXTRACTED))
                .thenReturn(List.of(own, someoneElses, unassigned));

        List<LoanProposal> result = facade.getLoanProposalsByStatus(LoanProposalStatus.EXTRACTED);

        assertThat(result).containsExactly(own, someoneElses, unassigned);
        verify(userFacade, never()).findUserByUsername(any());
    }

    // ---- accept / acceptAsPurchase / ignore / delete dziedzicza kontrole z getLoanProposalById ----

    @Test
    void accept_ShouldThrowAccessDenied_WhenRequestingUserIsNotAssignedOwner() {
        authenticateAs("john", "ROLE_FINANCE");
        LoanProposal proposal = proposal(1, 7, LoanProposalStatus.EXTRACTED);
        when(loanProposalRepository.findById(1)).thenReturn(Optional.of(proposal));
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(99L).build());

        assertThatThrownBy(() -> facade.accept(1, Loan.builder().build()))
                .isInstanceOf(AccessDeniedException.class);

        verify(addLoanUseCase, never()).addLoan(any());
    }

    @Test
    void delete_ShouldThrowAccessDenied_WhenRequestingUserIsNotAssignedOwner() {
        authenticateAs("john", "ROLE_FINANCE");
        LoanProposal proposal = proposal(1, 7, LoanProposalStatus.EXTRACTED);
        when(loanProposalRepository.findById(1)).thenReturn(Optional.of(proposal));
        when(userFacade.findUserByUsername("john")).thenReturn(AppUser.builder().id(99L).build());

        assertThatThrownBy(() -> facade.deleteLoanProposalById(1))
                .isInstanceOf(AccessDeniedException.class);

        verify(loanProposalRepository, never()).deleteById(anyInt());
    }
}
