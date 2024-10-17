package net.focik.homeoffice.finance.api;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.focik.homeoffice.finance.api.dto.BasicDto;
import net.focik.homeoffice.finance.api.dto.CardDto;
import net.focik.homeoffice.finance.api.mapper.ApiCardMapper;
import net.focik.homeoffice.finance.domain.card.Card;
import net.focik.homeoffice.finance.domain.card.port.primary.AddCardUseCase;
import net.focik.homeoffice.finance.domain.card.port.primary.DeleteCardUseCase;
import net.focik.homeoffice.finance.domain.card.port.primary.GetCardUseCase;
import net.focik.homeoffice.finance.domain.card.port.primary.UpdateCardUseCase;
import net.focik.homeoffice.utils.exceptions.ExceptionHandling;
import net.focik.homeoffice.utils.exceptions.HttpResponse;
import net.focik.homeoffice.utils.share.ActiveStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.OK;

@Log4j2
@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/finance/card")
//@CrossOrigin
public class CardController extends ExceptionHandling {

    private final ApiCardMapper mapper;
    private final AddCardUseCase addCardUseCase;
    private final UpdateCardUseCase updateCardUseCase;
    private final GetCardUseCase getCardUseCase;
    private final DeleteCardUseCase deleteCardUseCase;


    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('FINANCE_READ_ALL', 'FINANCE_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<CardDto> getById(@PathVariable int id) {
        log.info("Request to get card by id: {}", id);
        Card card = getCardUseCase.findById(id);

        if (card == null){
            log.warn("No card found with id: {}", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        log.info("Card found: {}", card);
        return new ResponseEntity<>(mapper.toDto(card), OK);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('FINANCE_READ_ALL') or hasAnyRole('ROLE_ADMIN', 'ROLE_FINANCE')")
    ResponseEntity<List<CardDto>> getAll(@RequestParam(required = false) ActiveStatus status) {
        log.info("Request to get all card with status: {} ", status);
        List<Card> cardsByStatus = getCardUseCase.findByStatus(status);
        log.info("Found {} cards with status:{}",cardsByStatus.size() , status);
        return new ResponseEntity<>(cardsByStatus.stream()
                .map(mapper::toDto)
                .collect(Collectors.toList()), OK);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyAuthority('FINANCE_READ_ALL', 'FINANCE_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<List<CardDto>> getByUser(@PathVariable int userId, @RequestParam(required = false) ActiveStatus status) {
        log.info("Request to get all card with status: {} for user with ID: {}", status, userId);
        List<Card> cardsByUser = getCardUseCase.findByUserAndStatus(userId, status);
        log.info("Found {} cards with status:{} for user with ID: {}",cardsByUser.size() , status, userId);
        return new ResponseEntity<>(cardsByUser.stream()
                .map(mapper::toDto)
                .collect(Collectors.toList()), OK);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('FINANCE_WRITE_ALL', 'FINANCE_WRITE') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<CardDto> addCard(@RequestBody CardDto cardDto) {
        log.info("Request to add a new card received with data: {}", cardDto);

        Card cardToAdd = mapper.toDomain(cardDto);
        log.debug("Mapped Card DTO to domain object: {}", cardToAdd);

        Card addedCard = addCardUseCase.addCard(cardToAdd);
        log.info("Card added successfully: {}", addedCard);

        return new ResponseEntity<>(mapper.toDto(addedCard), HttpStatus.CREATED);
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('FINANCE_WRITE_ALL', 'FINANCE_WRITE') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<CardDto> updateCard(@RequestBody CardDto cardDto) {
        log.info("Request to edit a card received with data: {}", cardDto);

        Card updatedCard = updateCardUseCase.updateCard(mapper.toDomain(cardDto));
        log.info("Card updated successfully: {}", updatedCard);
        return new ResponseEntity<>(mapper.toDto(updatedCard), OK);
    }

    @PutMapping("/status/{id}")
    @PreAuthorize("hasAnyAuthority('FINANCE_WRITE_ALL', 'FINANCE_WRITE') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<HttpResponse> updateCardStatus(@PathVariable int id, @RequestBody BasicDto basicDto) {
        log.info("Request to update card status with ID: {}, new status: {}", id, basicDto);

        updateCardUseCase.updateCardStatus(id, ActiveStatus.valueOf(basicDto.getValue()));
        log.info("Card status updated successfully");
        return response(HttpStatus.OK, "Zaaktualizowano status karty.");
    }

    @DeleteMapping("/{idCard}")
    @PreAuthorize("hasAnyAuthority('FINANCE_DELETE_ALL', 'FINANCE_DELETE') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<HttpResponse> deleteCard(@PathVariable int idCard) {
        log.info("Request to delete card with id: {}", idCard);

        deleteCardUseCase.deleteCard(idCard);
        log.info("Card with id: {} deleted successfully", idCard);

        return response(HttpStatus.NO_CONTENT, "Karta usunięty.");
    }

    private ResponseEntity<HttpResponse> response(HttpStatus status, String message) {
        HttpResponse body = new HttpResponse(status.value(), status, status.getReasonPhrase(), message);
        return new ResponseEntity<>(body, status);
    }
}
