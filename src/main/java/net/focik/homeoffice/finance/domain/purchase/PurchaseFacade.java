package net.focik.homeoffice.finance.domain.purchase;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.focik.homeoffice.audit.AuditAction;
import net.focik.homeoffice.audit.AuditLog;
import net.focik.homeoffice.finance.api.mapper.ApiPurchaseMapper;
import net.focik.homeoffice.finance.domain.card.Card;
import net.focik.homeoffice.finance.domain.card.CardFacade;
import net.focik.homeoffice.finance.domain.purchase.port.primary.AddPurchaseUseCase;
import net.focik.homeoffice.finance.domain.purchase.port.primary.DeletePurchaseUseCase;
import net.focik.homeoffice.finance.domain.purchase.port.primary.GetPurchaseUseCase;
import net.focik.homeoffice.finance.domain.purchase.port.primary.UpdatePurchaseUseCase;
import net.focik.homeoffice.finance.infrastructure.jpa.BankTransactionDtoRepository;
import net.focik.homeoffice.userservice.domain.AppUser;
import net.focik.homeoffice.userservice.domain.UserFacade;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Log4j2
@RequiredArgsConstructor
@Component
public class PurchaseFacade implements AddPurchaseUseCase, UpdatePurchaseUseCase, GetPurchaseUseCase, DeletePurchaseUseCase {


    private final PurchaseService purchaseService;
    private final UserFacade userFacade;
    private final BankTransactionDtoRepository bankTransactionRepository;
    private final ApiPurchaseMapper apiPurchaseMapper;
    private final CardFacade cardFacade;

    @Override
    @AuditLog(action = AuditAction.CREATE, entityType = "Purchase")
    public Purchase addPurchase(Purchase purchase) {
        return purchaseService.addPurchase(purchase);
    }

    @Override
    @AuditLog(action = AuditAction.UPDATE, entityType = "Purchase")
    public Purchase updatePurchaseStatus(int idPurchase, PaymentStatus paymentStatus) {
        Optional<Purchase> previousPurchase = Optional.ofNullable(purchaseService.findPurchaseById(idPurchase));
        PaymentStatus previousStatus = previousPurchase.orElseThrow().getPaymentStatus();

        Purchase purchase = previousPurchase.orElseThrow();
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
        return purchaseService.updatePurchase(purchase);
    }

    @Override
    @AuditLog(action = AuditAction.DELETE, entityType = "Purchase")
    public void deletePurchase(int id) {
        purchaseService.deletePurchase(id);
    }

    @Override
    public Purchase findById(int id) {
        return purchaseService.findPurchaseById(id);
    }

    @Override
    public List<Purchase> findByUser(String userName, PaymentStatus paymentStatus, LocalDate date) {
        AppUser user = userFacade.findUserByUsername(userName);
        return purchaseService.findPurchasesByUser(Math.toIntExact(user.getId()), paymentStatus);
    }

    @Override
    public List<Purchase> findByCard(int idCard) {
        return purchaseService.findByCard(idCard);
    }

    @Override
    public Map<LocalDate, List<Purchase>> findByUserMap(String userName, PaymentStatus paymentStatus, LocalDate date) {
        AppUser user = userFacade.findUserByUsername(userName);
        List<Purchase> purchasesByUser = purchaseService.findPurchasesByUser(Math.toIntExact(user.getId()), paymentStatus);
        return purchaseService.convertToMapByDeadline(purchasesByUser);
    }

    @Override
    public Map<LocalDate, List<Purchase>> findByUserMap(Integer userId, PaymentStatus paymentStatus, LocalDate date) {
        List<Purchase> purchasesByUser = purchaseService.findPurchasesByUser(userId, paymentStatus);
        return purchaseService.convertToMapByDeadline(purchasesByUser);
    }

    @Override
    public Map<LocalDate, List<Purchase>> findCurrent(String username) {
        AppUser user = userFacade.findUserByUsername(username);
        List<Purchase> currents = purchaseService.findCurrent(Math.toIntExact(user.getId()));
        return purchaseService.convertToMapByDeadline(currents);
    }

    @Override
    public List<Purchase> getPurchasesByFirm(Integer idFirm) {
        return purchaseService.getPurchasesByFirm(idFirm);
    }

    @Override
    public Page<Purchase> findPurchasesPageableWithFilters(int page, int size, String sortField, String sortDirection, String globalFilter, String username, String name, LocalDate purchaseDate, String dateComparisonType, PaymentStatus status, Integer idFirm, Integer idCard) {
        return purchaseService.findPurchasesPageableWithFilters(page, size, sortField, sortDirection, globalFilter, username, name, purchaseDate, dateComparisonType, status, idFirm, idCard);
    }

    @Override
    public Number getTotalSumToPay() {
        return purchaseService.getTotalSumToPay();
    }
}
