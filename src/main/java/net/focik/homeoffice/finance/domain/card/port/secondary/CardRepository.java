package net.focik.homeoffice.finance.domain.card.port.secondary;

import net.focik.homeoffice.finance.domain.card.Card;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public interface CardRepository {
    Card saveCard(Card card);

    Optional<Card> findCardById(Integer id);

    List<Card> findCardByUserId(Integer idUser);

    List<Card> findCardByName(String cardName);

    List<Card> findAll();

    void deleteCardById(int idCard);

    List<Card> findCardByBankId(Integer idBank);
}
