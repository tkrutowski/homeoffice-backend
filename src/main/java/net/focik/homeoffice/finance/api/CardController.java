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

        log.info("Try find card by id: " + id);

        Card card = getCardUseCase.findById(id);

        log.info(card != null ? "Found card for id = " + id : "Not found card for id = " + id);

        if (card == null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        return new ResponseEntity<>(mapper.toDto(card), OK);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('FINANCE_READ_ALL') or hasAnyRole('ROLE_ADMIN', 'ROLE_FINANCE')")
    ResponseEntity<List<CardDto>> getAll(@RequestParam(required = false) ActiveStatus status) {
        log.info("Try get all card for status " + status);

        List<Card> cardsByStatus = getCardUseCase.findByStatus(status);

        log.info("Found " + cardsByStatus.size() + " cards.");

        return new ResponseEntity<>(cardsByStatus.stream()
                .map(mapper::toDto)
                .collect(Collectors.toList()), OK);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyAuthority('FINANCE_READ_ALL', 'FINANCE_READ') or hasRole('ROLE_ADMIN')")
    ResponseEntity<List<CardDto>> getByUser(@PathVariable int userId, @RequestParam(required = false) ActiveStatus status) {
        log.info("Try get all card for status " + status);

        List<Card> cardsByUser = getCardUseCase.findByUserAndStatus(userId, status);

        log.info("Found " + cardsByUser.size() + " cards.");

        return new ResponseEntity<>(cardsByUser.stream()
                .map(mapper::toDto)
                .collect(Collectors.toList()), OK);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('FINANCE_WRITE_ALL', 'FINANCE_WRITE') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<CardDto> addCard(@RequestBody CardDto cardDto) {
        log.info("Try add new card.");

        Card card = mapper.toDomain(cardDto);
        Card result = addCardUseCase.addCard(card);

        log.info(result.getId() > 0 ? "Card added with id = " + result : "No card added!");

        if (result.getId() <= 0)
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);

        return new ResponseEntity<>(mapper.toDto(result), HttpStatus.CREATED);
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('FINANCE_WRITE_ALL', 'FINANCE_WRITE') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<CardDto> updateCard(@RequestBody CardDto cardDto) {
        log.info("Try update card with id: {}", cardDto.getId());

        Card card = updateCardUseCase.updateCard(mapper.toDomain(cardDto));
        return new ResponseEntity<>(mapper.toDto(card), OK);
    }

    @PutMapping("/status/{id}")
    @PreAuthorize("hasAnyAuthority('FINANCE_WRITE_ALL', 'FINANCE_WRITE') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<HttpResponse> updateCardStatus(@PathVariable int id, @RequestBody BasicDto basicDto) {
        log.info("Try update perchase status.");

        updateCardUseCase.updateCardStatus(id, ActiveStatus.valueOf(basicDto.getValue()));
        return response(HttpStatus.OK, "Zaaktualizowano status karty.");
    }

    @DeleteMapping("/{idCard}")
    @PreAuthorize("hasAnyAuthority('FINANCE_DELETE_ALL', 'FINANCE_DELETE') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<HttpResponse> deleteCard(@PathVariable int idCard) {
        log.info("Try delete card with id: " + idCard);

        deleteCardUseCase.deleteCard(idCard);

        log.info("Deleted bank with id = " + idCard);

        return response(HttpStatus.NO_CONTENT, "Karta usunięty.");
    }

    private ResponseEntity<HttpResponse> response(HttpStatus status, String message) {
        HttpResponse body = new HttpResponse(status.value(), status, status.getReasonPhrase(), message);
        return new ResponseEntity<>(body, status);
    }
}
