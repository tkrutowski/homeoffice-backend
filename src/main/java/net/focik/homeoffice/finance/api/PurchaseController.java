package net.focik.homeoffice.finance.api;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.focik.homeoffice.finance.api.dto.BasicDto;
import net.focik.homeoffice.finance.api.dto.PurchaseDto;
import net.focik.homeoffice.finance.api.mapper.ApiPurchaseMapper;
import net.focik.homeoffice.finance.domain.purchase.Purchase;
import net.focik.homeoffice.finance.domain.purchase.port.primary.AddPurchaseUseCase;
import net.focik.homeoffice.finance.domain.purchase.port.primary.DeletePurchaseUseCase;
import net.focik.homeoffice.finance.domain.purchase.port.primary.GetPurchaseUseCase;
import net.focik.homeoffice.finance.domain.purchase.port.primary.UpdatePurchaseUseCase;
import net.focik.homeoffice.utils.UserHelper;
import net.focik.homeoffice.utils.exceptions.ExceptionHandling;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.OK;

@Log4j2
@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/finance/purchase")
//@CrossOrigin
public class PurchaseController extends ExceptionHandling {

    private final ApiPurchaseMapper mapper;
    private final AddPurchaseUseCase addPurchaseUseCase;
    private final UpdatePurchaseUseCase updatePurchaseUseCase;
    private final GetPurchaseUseCase getPurchaseUseCase;
    private final DeletePurchaseUseCase deletePurchaseUseCase;


    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    ResponseEntity<PurchaseDto> getById(@PathVariable int id) {
        log.info("Request to get purchase by id: {}", id);

        Purchase purchase = getPurchaseUseCase.findById(id);

        if (purchase == null) {
            log.warn("No purchase found with id: {}", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        log.info("Purchase found: {}", purchase);
        PurchaseDto dto = mapper.toDto(purchase);
        log.info("Mapped to Purchase DTO found: {}", dto);
        return new ResponseEntity<>(dto, OK);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    ResponseEntity<Map<String, List<PurchaseDto>>> getAll(@RequestParam PaymentStatus status,
                                                          @RequestParam(required = false) LocalDate date) {
        log.info("Request to get all purchase with status: {} and date: {}", status, date);

        Map<LocalDate, List<Purchase>> purchaseMap = getPurchaseUseCase.findByUserMap(UserHelper.getUserName(), status, date);
        log.info("Found {} purchases.", purchaseMap.size());

        return new ResponseEntity<>(mapper.toDto(purchaseMap), OK);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    ResponseEntity<Map<String, List<PurchaseDto>>> getByUser(@PathVariable int userId, @RequestParam(required = false) PaymentStatus status,
                                                             @RequestParam(required = false) LocalDate date) {
        log.info("Request to get all purchase with status: {} and date: {} for user with ID: {}", status, date, userId);

        Map<LocalDate, List<Purchase>> purchaseMap = getPurchaseUseCase.findByUserMap(userId, status, date);
        log.info("Found {} purchases.", purchaseMap.size());

        return new ResponseEntity<>(mapper.toDto(purchaseMap), OK);
    }

    @GetMapping("/current/{username}")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    ResponseEntity<Map<String, List<PurchaseDto>>> getCurrent(@PathVariable String username) {
        log.info("Request to get current purchases for user: {}", username );

        Map<LocalDate, List<Purchase>> purchaseMap = getPurchaseUseCase.findCurrent(username);
        log.info("Found {} purchases.", purchaseMap.size());

        return new ResponseEntity<>(mapper.toDto(purchaseMap), OK);
    }

    @GetMapping("/sum/to-pay")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    ResponseEntity<Number> getTotalSumToPay() {
        log.info("Request to get total sum to pay.");

        Number total = getPurchaseUseCase.getTotalSumToPay();
        log.info("Found total sum to pay: {}", total);

        return new ResponseEntity<>(total, OK);
    }

    @GetMapping("/page")
    ResponseEntity<Page<PurchaseDto>> getPurchasesPage(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", defaultValue = "purchaseDate") String sortField,
            @RequestParam(name = "direction", defaultValue = "DESC") String sortDirection,
            @RequestParam(name = "globalFilter", required = false) String globalFilter,
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "purchaseDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate purchaseDate,
            @RequestParam(name = "dateComparisonType", required = false) String dateComparisonType,
            @RequestParam(name = "status", required = false) PaymentStatus status,
            @RequestParam(name = "firmId", required = false) Integer idFirm,
            @RequestParam(name = "cardId", required = false) Integer idCard
    ) {
        log.info("Request to get purchases page with page: {}, size: {}, sort: {}, direction: {}, globalFilter: {}, username: {}, name: {}, purchaseDate: {}, dateComparisonType: {},  status: {}, firmId: {}, cardId: {}",
                page, size, sortField, sortDirection, globalFilter, username, name, purchaseDate, dateComparisonType,  status, idFirm, idCard);

        Page<Purchase> purchasesPage = getPurchaseUseCase.findPurchasesPageableWithFilters(
                page, size, sortField, sortDirection, globalFilter, username, name, purchaseDate,dateComparisonType, status, idFirm, idCard);

        Page<PurchaseDto> dtoPage = purchasesPage.map(mapper::toDto);

        log.debug("Found {} purchases on page {} of {}",
                dtoPage.getNumberOfElements(),
                dtoPage.getNumber(),
                dtoPage.getTotalPages());

        return ResponseEntity.ok(dtoPage);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public ResponseEntity<PurchaseDto> addPurchase(@RequestBody PurchaseDto purchaseDto) {
        log.info("Try add new purchase.");

        Purchase purchase = mapper.toDomain(purchaseDto);
        Purchase result = addPurchaseUseCase.addPurchase(purchase);

        log.info(result.getId() > 0 ? "purchase added with id = " + result : "No purchase added!");

        if (result.getId() <= 0)
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);

        return new ResponseEntity<>(mapper.toDto(result), HttpStatus.CREATED);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public ResponseEntity<PurchaseDto> updatePurchase(@RequestBody PurchaseDto purchaseDto) {
        log.info("Try update purchase with id: {}", purchaseDto.getId());

        Purchase card = updatePurchaseUseCase.updatePurchase(mapper.toDomain(purchaseDto));
        return new ResponseEntity<>(mapper.toDto(card), OK);
    }

    @PutMapping("/status/{id}")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public ResponseEntity<PurchaseDto> updatePurchaseStatus(@PathVariable int id, @RequestBody BasicDto basicDto) {
        log.info("Try update purchase status.");

        Purchase purchase = updatePurchaseUseCase.updatePurchaseStatus(id, PaymentStatus.valueOf(basicDto.getValue()));
        return new ResponseEntity<>(mapper.toDto(purchase), OK);
    }

    @DeleteMapping("/{idPurchase}")
    @PreAuthorize("hasAnyRole('ROLE_FINANCE', 'ROLE_ADMIN')")
    public void deletePurchase(@PathVariable int idPurchase) {
        log.info("Try purchase card with id: {}", idPurchase);

        deletePurchaseUseCase.deletePurchase(idPurchase);

        log.info("Deleted purchase with id = {}", idPurchase);
    }

}
