package net.focik.homeoffice.finance.domain.card;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.finance.domain.card.port.secondary.CardRepository;
import net.focik.homeoffice.finance.domain.exception.CardAlreadyExistException;
import net.focik.homeoffice.finance.domain.exception.CardCanNotBeDeletedException;
import net.focik.homeoffice.finance.domain.exception.CardNotFoundException;
import net.focik.homeoffice.finance.domain.exception.CardNotValidException;
import net.focik.homeoffice.finance.domain.purchase.Purchase;
import net.focik.homeoffice.finance.domain.purchase.PurchaseFacade;
import net.focik.homeoffice.utils.FileHelper;
import net.focik.homeoffice.utils.share.ActiveStatus;
import net.focik.homeoffice.utils.share.Module;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
class CardService {

    private final CardRepository cardRepository;
    private final PurchaseFacade purchaseFacade;
    private final FileHelper fileHelper;


    Card addCard(Card card) {
        log.debug("Adding card {}", card);
        if (isNotValid(card))
            throw new CardNotValidException();
        if(!cardRepository.findCardByName(card.getCardName()).isEmpty())
            throw new CardAlreadyExistException("Karta o tej nazwie już istnieje.");

        card.setImageUrl(fileHelper.downloadAndSaveImage(card.getImageUrl(), card.getCardName(), Module.CARD));
        return cardRepository.saveCard(card);
    }

    List<Card> findCardsByUserAndStatus(Integer idUser, ActiveStatus activeStatus) {
        log.debug("Finding cards by user {} and status {}", idUser, activeStatus);
        List<Card> cardByUserId = cardRepository.findCardByUserId(idUser);

        if (activeStatus == null || activeStatus == ActiveStatus.ALL)
            return cardByUserId;

        cardByUserId = cardByUserId.stream()
                .filter(card -> card.getActiveStatus().equals(activeStatus))
                .collect(Collectors.toList());

        log.debug("Found {} cards", cardByUserId);
        return cardByUserId;
    }

    List<Card> findCardsByStatus(ActiveStatus activeStatus) {
        log.debug("Finding cards by status {}", activeStatus);
        List<Card> cardList = cardRepository.findAll();

        if (activeStatus == null || activeStatus == ActiveStatus.ALL)
            return cardList;

        cardList = cardList.stream()
                .filter(card -> card.getActiveStatus().equals(activeStatus))
                .collect(Collectors.toList());

        log.debug("Found {} cards", cardList.size());
        return cardList;
    }

    Card findCardById(Integer idCard) {
        log.debug("Finding card by id {}", idCard);
        Optional<Card> cardById = cardRepository.findCardById(idCard);

        if (cardById.isEmpty()) {
            log.debug("Card with id {} not found", idCard);
            throw new CardNotFoundException(idCard);
        }

        log.debug("Found card {}", cardById.get());
        return cardById.get();
    }

    @Transactional
    public void deleteCard(Integer idCard) {
        log.debug("Deleting card {}", idCard);
        List<Purchase> byCard = purchaseFacade.findByCard(idCard);
        if (!byCard.isEmpty()) {
            log.debug("Card can not be deleted because there are purchases linked to it.");
            throw new CardCanNotBeDeletedException("zakupy. (" + byCard.size() + ")");
        }
        cardRepository.deleteCardById(idCard);
        log.debug("Card with id {} deleted", idCard);
    }

    public Card updateCard(Card card) {
        log.debug("Updating card {}", card);
        if (isNotValid(card))
            throw new CardNotValidException();
        return cardRepository.saveCard(card);
    }

    private boolean isNotValid(Card card) {
        log.debug("Checking if card is not valid");
        if (card.getLimit() == 0)
            return true;
        return card.getActivationDate() == null && card.getExpirationDate() == null;
    }

    public List<Card> findCardsByBank(Integer idBank) {
        log.debug("Finding cards by bankId {}", idBank);
        return cardRepository.findCardByBankId(idBank);
    }
}