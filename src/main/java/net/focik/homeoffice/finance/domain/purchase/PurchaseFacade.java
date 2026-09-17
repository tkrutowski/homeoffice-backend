package net.focik.homeoffice.finance.domain.purchase;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.focik.homeoffice.audit.AuditAction;
import net.focik.homeoffice.audit.AuditLog;
import net.focik.homeoffice.finance.api.mapper.ApiPurchaseMapper;
import net.focik.homeoffice.finance.domain.card.Card;
import net.focik.homeoffice.finance.domain.card.CardFacade;
import net.focik.homeoffice.finance.domain.card.PaymentDeadlineCalculator;
import net.focik.homeoffice.finance.domain.purchase.port.primary.AddPurchaseUseCase;
import net.focik.homeoffice.finance.domain.purchase.port.primary.CalculatePaymentDeadlineUseCase;
import net.focik.homeoffice.finance.domain.purchase.port.primary.DeletePurchaseUseCase;
import net.focik.homeoffice.finance.domain.purchase.port.primary.GetPurchaseUseCase;
import net.focik.homeoffice.finance.domain.purchase.port.primary.UpdatePurchaseUseCase;
import net.focik.homeoffice.finance.infrastructure.jpa.BankTransactionDtoRepository;
import net.focik.homeoffice.userservice.domain.AppUser;
import net.focik.homeoffice.userservice.domain.UserFacade;
import net.focik.homeoffice.utils.UserHelper;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static net.focik.homeoffice.utils.PrivilegeHelper.*;

@Log4j2
@RequiredArgsConstructor
@Component
public class PurchaseFacade implements AddPurchaseUseCase, UpdatePurchaseUseCase, GetPurchaseUseCase, DeletePurchaseUseCase, CalculatePaymentDeadlineUseCase {


    private final PurchaseService purchaseService;
    private final UserFacade userFacade;
    private final BankTransactionDtoRepository bankTransactionRepository;
    private final ApiPurchaseMapper apiPurchaseMapper;
    private final CardFacade cardFacade;

    @Override
    @AuditLog(action = AuditAction.CREATE, entityType = "Purchase")
    public Purchase addPurchase(Purchase purchase) {
        enforceOwnIdUserUnlessPrivilegedToWriteAll(purchase);
        return purchaseService.addPurchase(purchase);
    }

    @Override
    @AuditLog(action = AuditAction.UPDATE, entityType = "Purchase")
    public Purchase updatePurchaseStatus(int idPurchase, PaymentStatus paymentStatus) {
        Optional<Purchase> previousPurchase = Optional.ofNullable(purchaseService.findPurchaseById(idPurchase));
        PaymentStatus previousStatus = previousPurchase.orElseThrow().getPaymentStatus();

        Purchase purchase = previousPurchase.orElseThrow();
        assertCanWritePurchase(purchase);
        purchase.changePaymentStatus(paymentStatus);
        if (paymentStatus == PaymentStatus.TO_PAY) {
            purchase.setPaymentDate(null);
        }

        Purchase result = purchaseService.updatePurchase(purchase);

        if (previousStatus != PaymentStatus.PAID && paymentStatus == PaymentStatus.PAID && purchase.getPaymentDate() != null) {

            var bankTransaction = apiPurchaseMapper.toBankTransaction(purchase, findCategoryByCard(purchase.getIdCard()));
            bankTransactionRepository.save(bankTransaction);

            log.info("Purchase paid: purchaseId={}, amount={}, date={}, transactionId={}",
                    purchase.getId(),
                    purchase.getAmount().doubleValue(),
                    purchase.getPaymentDate(),
                    bankTransaction.getId());
        }

        return result;
    }

    private int findCategoryByCard(int idCard) {
        Card card = cardFacade.findById(idCard);
        return switch (card.getCardName().toLowerCase()) {
            case "alfa" -> 8;
            case "impresja" -> 9;
            default -> throw new IllegalArgumentException("Unknown card name: " + card.getCardName());
        };

    }

    @Override
    @AuditLog(action = AuditAction.UPDATE, entityType = "Purchase")
    public Purchase updatePurchase(Purchase purchase) {
        Purchase existingPurchase = purchaseService.findPurchaseById(purchase.getId());
        assertCanWritePurchase(existingPurchase);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && !canWriteAllPurchases(authentication)) {
            // bez uprawnien do zarzadzania cudzymi zakupami nie mozna przepisac zakupu na inna osobe
            purchase.setIdUser(existingPurchase.getIdUser());
        }

        return purchaseService.updatePurchase(purchase);
    }

    @Override
    @AuditLog(action = AuditAction.DELETE, entityType = "Purchase")
    public void deletePurchase(int id) {
        Purchase purchase = purchaseService.findPurchaseById(id);
        assertCanDeletePurchase(purchase);

        purchaseService.deletePurchase(id);
    }

    @Override
    public Purchase findById(int id) {
        Purchase purchase = purchaseService.findPurchaseById(id);
        assertCanReadPurchase(purchase);
        return purchase;
    }

    @Override
    public List<Purchase> findAllById(List<Integer> ids) {
        return purchaseService.findAllById(ids);
    }

    @Override
    public List<Purchase> findByLoan(int idLoan) {
        return purchaseService.findByLoan(idLoan);
    }

    @Override
    public List<Purchase> findByUser(String userName, PaymentStatus paymentStatus, LocalDate date) {
        assertCanReadUsername(userName);
        AppUser user = userFacade.findUserByUsername(userName);
        return purchaseService.findPurchasesByUser(Math.toIntExact(user.getId()), paymentStatus);
    }

    @Override
    public List<Purchase> findByCard(int idCard) {
        return purchaseService.findByCard(idCard);
    }

    @Override
    public Map<LocalDate, List<Purchase>> findByUserMap(String userName, PaymentStatus paymentStatus, LocalDate date) {
        assertCanReadUsername(userName);
        AppUser user = userFacade.findUserByUsername(userName);
        List<Purchase> purchasesByUser = purchaseService.findPurchasesByUser(Math.toIntExact(user.getId()), paymentStatus);
        return purchaseService.convertToMapByDeadline(purchasesByUser);
    }

    @Override
    public Map<LocalDate, List<Purchase>> findByUserMap(Integer userId, PaymentStatus paymentStatus, LocalDate date) {
        assertCanReadUserId(userId);
        List<Purchase> purchasesByUser = purchaseService.findPurchasesByUser(userId, paymentStatus);
        return purchaseService.convertToMapByDeadline(purchasesByUser);
    }

    @Override
    public Map<LocalDate, List<Purchase>> findCurrent(int userId) {
        assertCanReadUserId(userId);
        List<Purchase> currents = purchaseService.findCurrent(userId);
        return purchaseService.convertToMapByDeadline(currents);
    }

    @Override
    public List<Purchase> getPurchasesByFirm(Integer idFirm) {
        return purchaseService.getPurchasesByFirm(idFirm);
    }

    @Override
    public Page<Purchase> findPurchasesPageableWithFilters(int page, int size, String sortField, String sortDirection, String globalFilter, Integer userId, String name, LocalDate purchaseDate, String dateComparisonType, PaymentStatus status, Integer idFirm, Integer idCard) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        // Zwykly uzytkownik nie moze wymusic filtra userId na cudze konto - nadpisujemy go
        // wlasnym, niezaleznie od tego, co przyszlo z requestu.
        if (authentication != null && !canReadAllPurchases(authentication)) {
            AppUser user = userFacade.findUserByUsername(UserHelper.getUserName());
            userId = Math.toIntExact(user.getId());
        }

        return purchaseService.findPurchasesPageableWithFilters(page, size, sortField, sortDirection, globalFilter, userId, name, purchaseDate, dateComparisonType, status, idFirm, idCard);
    }

    @Override
    public Number getTotalSumToPay() {
        return purchaseService.getTotalSumToPay();
    }

    @Override
    public LocalDate calculatePaymentDeadline(int idCard, LocalDate purchaseDate) {
        Card card = cardFacade.findById(idCard);
        return PaymentDeadlineCalculator.calculate(card, purchaseDate);
    }

    /**
     * Rzuca {@link AccessDeniedException}, jesli aktualnie zalogowany uzytkownik nie ma uprawnien
     * do przegladania wszystkich zakupow (READ_ALL), a pyta o dane innego uzytkownika (po nazwie)
     * niz on sam.
     */
    private void assertCanReadUsername(String requestedUsername) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || canReadAllPurchases(authentication)) {
            return;
        }

        if (!UserHelper.getUserName().equals(requestedUsername)) {
            throw new AccessDeniedException("Brak uprawnień do danych tego użytkownika.");
        }
    }

    /**
     * Jak {@link #assertCanReadUsername}, ale dla identyfikatora liczbowego uzytkownika.
     */
    private void assertCanReadUserId(int requestedUserId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || canReadAllPurchases(authentication)) {
            return;
        }

        AppUser user = userFacade.findUserByUsername(UserHelper.getUserName());
        if (requestedUserId != Math.toIntExact(user.getId())) {
            throw new AccessDeniedException("Brak uprawnień do danych tego użytkownika.");
        }
    }

    /**
     * Rzuca {@link AccessDeniedException}, jesli aktualnie zalogowany uzytkownik nie ma uprawnien
     * do przegladania wszystkich zakupow (READ_ALL), a podany zakup nie nalezy do niego.
     */
    private void assertCanReadPurchase(Purchase purchase) {
        assertCanAccessPurchase(purchase, this::canReadAllPurchases);
    }

    /**
     * Rzuca {@link AccessDeniedException}, jesli aktualnie zalogowany uzytkownik nie ma uprawnien
     * do zarzadzania wszystkimi zakupami (WRITE_ALL), a podany zakup nie nalezy do niego.
     */
    private void assertCanWritePurchase(Purchase purchase) {
        assertCanAccessPurchase(purchase, this::canWriteAllPurchases);
    }

    /**
     * Rzuca {@link AccessDeniedException}, jesli aktualnie zalogowany uzytkownik nie ma uprawnien
     * do usuwania wszystkich zakupow (DELETE_ALL), a podany zakup nie nalezy do niego.
     */
    private void assertCanDeletePurchase(Purchase purchase) {
        assertCanAccessPurchase(purchase, this::canDeleteAllPurchases);
    }

    private void assertCanAccessPurchase(Purchase purchase, Predicate<Authentication> hasFullAccess) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        // Brak kontekstu security (np. scheduler) - traktujemy jak pelny dostep
        if (authentication == null || hasFullAccess.test(authentication)) {
            return;
        }

        AppUser user = userFacade.findUserByUsername(UserHelper.getUserName());
        if (purchase.getIdUser() != Math.toIntExact(user.getId())) {
            throw new AccessDeniedException("Brak uprawnień do tego zakupu.");
        }
    }

    /**
     * Wymusza idUser = aktualnie zalogowany uzytkownik, ignorujac wartosc przeslana z klienta,
     * chyba ze uzytkownik ma uprawnienie do zarzadzania cudzymi zakupami.
     */
    private void enforceOwnIdUserUnlessPrivilegedToWriteAll(Purchase purchase) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && !canWriteAllPurchases(authentication)) {
            AppUser user = userFacade.findUserByUsername(UserHelper.getUserName());
            purchase.setIdUser(Math.toIntExact(user.getId()));
        }
    }

    private boolean canReadAllPurchases(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(ROLE_ADMIN)
                        || grantedAuthority.getAuthority().equals(FINANCE_PURCHASE_READ_ALL));
    }

    private boolean canWriteAllPurchases(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(ROLE_ADMIN)
                        || grantedAuthority.getAuthority().equals(FINANCE_PURCHASE_WRITE_ALL));
    }

    private boolean canDeleteAllPurchases(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(ROLE_ADMIN)
                        || grantedAuthority.getAuthority().equals(FINANCE_PURCHASE_DELETE_ALL));
    }
}
