package net.focik.homeoffice.finance.domain.card;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.finance.domain.card.port.secondary.CardRepository;
import net.focik.homeoffice.finance.domain.exception.CardAlreadyExistException;
import net.focik.homeoffice.finance.domain.exception.CardCanNotBeDeletedException;
import net.focik.homeoffice.finance.domain.exception.CardNotFoundException;
import net.focik.homeoffice.finance.domain.exception.CardNotValidException;
import net.focik.homeoffice.finance.domain.purchase.Purchase;
import net.focik.homeoffice.finance.domain.purchase.PurchaseFacade;
import net.focik.homeoffice.utils.share.ActiveStatus;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
class CardService {

    private final CardRepository cardRepository;
    private final PurchaseFacade purchaseFacade;
    @Value("${cards.directory}")
    private final String cardCatalogUrl;
    @Value("${cards.url}")
    private final String homeUrl;

    public CardService(CardRepository cardRepository, PurchaseFacade purchaseFacade, @Value("${cards.directory}")String cardCatalogUrl, @Value("${cards.url}")String homeUrl) {
        this.cardRepository = cardRepository;
        this.purchaseFacade = purchaseFacade;
        this.cardCatalogUrl = cardCatalogUrl;
        this.homeUrl = homeUrl;
    }

    Card addCard(Card card) {
        log.debug("Adding card {}", card);
        if (isNotValid(card))
            throw new CardNotValidException();
        if(!cardRepository.findCardByName(card.getCardName()).isEmpty())
            throw new CardAlreadyExistException("Karta o tej nazwie już istnieje.");

        card.setImageUrl(downloadAndSaveImage(card.getImageUrl(), card.getCardName()));
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

    public String downloadAndSaveImage(String imageUrl, String name) {
        try {
            log.debug("Downloading image {}", imageUrl);
            URI uri = new URI(imageUrl);
            URL url = uri.toURL();

            // Pobieranie rozszerzenia pliku z URL
            String path = url.getPath();
            String extension = path.substring(path.lastIndexOf("."));

            String fileName = "image_" + name.trim().replace(" ", "_") + UUID.randomUUID() + extension; // Generowanie unikalnej nazwy pliku
            File outputFile = new File(cardCatalogUrl + "/" + fileName);
            log.debug("Saving image {} in {}", fileName, outputFile);
            // Pobierz plik z URL i zapisz go na dysku
            FileUtils.copyURLToFile(url, outputFile, 10000, 10000);

            log.debug("URL saved file: {}", homeUrl + fileName);
            return homeUrl + fileName;
        } catch (IOException e) {
            log.error("Error downloading ans saving image (return null)",e);
            return null;
        } catch (URISyntaxException e) {
            log.error("Error downloading ans saving image",e);
            throw new RuntimeException(e);
        }
    }
}