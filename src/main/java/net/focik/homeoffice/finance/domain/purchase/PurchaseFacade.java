package net.focik.homeoffice.finance.domain.purchase;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.finance.domain.purchase.port.primary.AddPurchaseUseCase;
import net.focik.homeoffice.finance.domain.purchase.port.primary.DeletePurchaseUseCase;
import net.focik.homeoffice.finance.domain.purchase.port.primary.GetPurchaseUseCase;
import net.focik.homeoffice.finance.domain.purchase.port.primary.UpdatePurchaseUseCase;
import net.focik.homeoffice.userservice.domain.AppUser;
import net.focik.homeoffice.userservice.domain.UserFacade;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
@AllArgsConstructor
public class PurchaseFacade implements AddPurchaseUseCase, UpdatePurchaseUseCase, GetPurchaseUseCase, DeletePurchaseUseCase {

    private final PurchaseService purchaseService;
    private final UserFacade userFacade;

    @Override
    public Purchase addPurchase(Purchase purchase) {
        return purchaseService.addPurchase(purchase);
    }

    @Override
    public Purchase updatePurchaseStatus(int idPurchase, PaymentStatus paymentStatus) {
        Purchase purchase = purchaseService.findPurchaseById(idPurchase);
        purchase.changePaymentStatus(paymentStatus);

        return purchaseService.updatePurchase(purchase);
    }

    @Override
    public Purchase updatePurchase(Purchase purchase) {
        return purchaseService.updatePurchase(purchase);
    }

    @Override
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
        return purchaseService.findPurchasesByUser(Math.toIntExact(user.getId()), paymentStatus, date);
    }

    @Override
    public List<Purchase> findByCard(int idCard) {
        return purchaseService.findByCard(idCard);
    }

    @Override
    public Map<LocalDate, List<Purchase>> findByUserMap(String userName, PaymentStatus paymentStatus, LocalDate date) {
        AppUser user = userFacade.findUserByUsername(userName);
        List<Purchase> purchasesByUser = purchaseService.findPurchasesByUser(Math.toIntExact(user.getId()), paymentStatus, date);
        return purchaseService.convertToMapByDeadline(purchasesByUser);
    }

    @Override
    public Map<LocalDate, List<Purchase>> findByUserMap(Integer userId, PaymentStatus paymentStatus, LocalDate date) {
        List<Purchase> purchasesByUser = purchaseService.findPurchasesByUser(userId, paymentStatus, date);
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
    public Page<Purchase> findPurchasesPageableWithFilters(int page, int size, String sortField, String sortDirection, String globalFilter, String username, String name, LocalDate purchaseDate, String dateComparisonType, String firmName, PaymentStatus status) {
        return purchaseService.findPurchasesPageableWithFilters(page, size, sortField, sortDirection, globalFilter, username, name, purchaseDate, dateComparisonType, firmName, status);
    }

    @Override
    public Number getTotalSumToPay() {
        return purchaseService.getTotalSumToPay();
    }
}
