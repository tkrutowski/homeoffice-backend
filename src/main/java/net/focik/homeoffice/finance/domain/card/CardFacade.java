package net.focik.homeoffice.finance.domain.card;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.finance.domain.card.port.primary.AddCardUseCase;
import net.focik.homeoffice.finance.domain.card.port.primary.DeleteCardUseCase;
import net.focik.homeoffice.finance.domain.card.port.primary.GetCardUseCase;
import net.focik.homeoffice.finance.domain.card.port.primary.UpdateCardUseCase;
import net.focik.homeoffice.userservice.domain.AppUser;
import net.focik.homeoffice.userservice.domain.UserFacade;
import net.focik.homeoffice.utils.UserHelper;
import net.focik.homeoffice.utils.share.ActiveStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
public class CardFacade implements AddCardUseCase, UpdateCardUseCase, GetCardUseCase, DeleteCardUseCase {

    private final CardService cardService;
    private final UserFacade userFacade;

    @Override
    public Card addCard(Card card) {
        return cardService.addCard(card);
    }

    @Override
    public void updateCardStatus(int idCard, ActiveStatus activeStatus) {
        Card card = cardService.findCardById(idCard);
        card.changeActiveStatus(activeStatus);

        cardService.updateCard(card);
    }

    @Override
    public Card updateCard(Card card) {
        return cardService.updateCard(card);
    }

    @Override
    public void deleteCard(int id) {
        cardService.deleteCard(id);
    }

    @Override
    public Card findById(int id) {
        return cardService.findCardById(id);
    }

    @Override
    public List<Card> findByStatus(ActiveStatus activeStatus) {
        return cardService.findCardsByStatus(activeStatus);
    }

    @Override
    public List<Card> findByUserAndStatus(Integer userId, ActiveStatus status) {
        AppUser user = userFacade.findUserByUsername(UserHelper.getUserName());
        return cardService.findCardsByUserAndStatus(userId, status);
    }

    @Override
    public List<Card> getCardsByBank(Integer idBank) {
        return cardService.findCardsByBank(idBank);

    }
}
