package net.focik.homeoffice.finance.domain.card;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.audit.AuditAction;
import net.focik.homeoffice.audit.AuditLog;
import net.focik.homeoffice.finance.domain.card.port.primary.AddCardUseCase;
import net.focik.homeoffice.finance.domain.card.port.primary.DeleteCardUseCase;
import net.focik.homeoffice.finance.domain.card.port.primary.GetCardUseCase;
import net.focik.homeoffice.finance.domain.card.port.primary.UpdateCardUseCase;
import net.focik.homeoffice.userservice.domain.AppUser;
import net.focik.homeoffice.userservice.domain.UserFacade;
import net.focik.homeoffice.utils.UserHelper;
import net.focik.homeoffice.utils.share.ActiveStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

import static net.focik.homeoffice.utils.PrivilegeHelper.*;

@Component
@AllArgsConstructor
public class CardFacade implements AddCardUseCase, UpdateCardUseCase, GetCardUseCase, DeleteCardUseCase {

    private final CardService cardService;
    private final UserFacade userFacade;

    @Override
    @AuditLog(action = AuditAction.CREATE, entityType = "Card")
    public Card addCard(Card card) {
        enforceOwnIdUserUnlessPrivilegedToWriteAll(card);
        return cardService.addCard(card);
    }

    @Override
    @AuditLog(action = AuditAction.UPDATE, entityType = "Card")
    public void updateCardStatus(int idCard, ActiveStatus activeStatus) {
        Card card = cardService.findCardById(idCard);
        assertCanWriteCard(card);
        card.changeActiveStatus(activeStatus);

        cardService.updateCard(card);
    }

    @Override
    @AuditLog(action = AuditAction.UPDATE, entityType = "Card")
    public Card updateCard(Card card) {
        Card existingCard = cardService.findCardById(card.getId());
        assertCanWriteCard(existingCard);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && !canWriteAllCards(authentication)) {
            // bez uprawnien do zarzadzania cudzymi kartami nie mozna przepisac karty na inna osobe
            card.setIdUser(existingCard.getIdUser());
        }

        return cardService.updateCard(card);
    }

    @Override
    @AuditLog(action = AuditAction.DELETE, entityType = "Card")
    public void deleteCard(int id) {
        Card card = cardService.findCardById(id);
        assertCanDeleteCard(card);

        cardService.deleteCard(id);
    }

    @Override
    public Card findById(int id) {
        Card card = cardService.findCardById(id);
        assertCanReadCard(card);
        return card;
    }

    @Override
    public List<Card> findByStatus(ActiveStatus activeStatus) {
        List<Card> cards = cardService.findCardsByStatus(activeStatus);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || canReadAllCards(authentication)) {
            return cards;
        }

        int currentUserId = currentUserId();
        return cards.stream()
                .filter(card -> card.getIdUser() == currentUserId)
                .toList();
    }

    @Override
    public List<Card> findByUserAndStatus(Integer userId, ActiveStatus status) {
        assertCanReadUserId(userId);
        return cardService.findCardsByUserAndStatus(userId, status);
    }

    @Override
    public List<Card> getCardsByBank(Integer idBank) {
        return cardService.findCardsByBank(idBank);

    }

    @Override
    public List<Card> findAll() {
        return cardService.findAllCards();
    }

    /**
     * Rzuca {@link AccessDeniedException}, jesli aktualnie zalogowany uzytkownik nie ma uprawnien
     * do przegladania wszystkich kart (FINANCE_READ_ALL), a pyta o dane innego uzytkownika niz on sam.
     */
    private void assertCanReadUserId(int requestedUserId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || canReadAllCards(authentication)) {
            return;
        }

        if (requestedUserId != currentUserId()) {
            throw new AccessDeniedException("Brak uprawnień do danych tego użytkownika.");
        }
    }

    private void assertCanReadCard(Card card) {
        assertCanAccessCard(card, this::canReadAllCards);
    }

    private void assertCanWriteCard(Card card) {
        assertCanAccessCard(card, this::canWriteAllCards);
    }

    private void assertCanDeleteCard(Card card) {
        assertCanAccessCard(card, this::canDeleteAllCards);
    }

    private void assertCanAccessCard(Card card, Predicate<Authentication> hasFullAccess) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        // Brak kontekstu security (np. scheduler) - traktujemy jak pelny dostep
        if (authentication == null || hasFullAccess.test(authentication)) {
            return;
        }

        if (card.getIdUser() != currentUserId()) {
            throw new AccessDeniedException("Brak uprawnień do tej karty.");
        }
    }

    private void enforceOwnIdUserUnlessPrivilegedToWriteAll(Card card) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && !canWriteAllCards(authentication)) {
            card.setIdUser(currentUserId());
        }
    }

    private int currentUserId() {
        AppUser user = userFacade.findUserByUsername(UserHelper.getUserName());
        return Math.toIntExact(user.getId());
    }

    private boolean canReadAllCards(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(ROLE_ADMIN)
                        || grantedAuthority.getAuthority().equals(FINANCE_READ_ALL));
    }

    private boolean canWriteAllCards(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(ROLE_ADMIN)
                        || grantedAuthority.getAuthority().equals(FINANCE_WRITE_ALL));
    }

    private boolean canDeleteAllCards(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(ROLE_ADMIN)
                        || grantedAuthority.getAuthority().equals(FINANCE_DELETE_ALL));
    }
}
