package net.focik.homeoffice.finance.domain.purchase.port.secondary;

import net.focik.homeoffice.finance.domain.purchase.Purchase;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public interface PurchaseRepository {
    Purchase savePurchase(Purchase card);

    Optional<Purchase> findPurchaseById(Integer id);

    List<Purchase> findPurchaseByUserId(Integer idUser);

    List<Purchase> findPurchaseByUserAndStatus(Integer idUser, PaymentStatus status);

    List<Purchase> findPurchaseByUserIdAndDeadline(Integer idUser, LocalDate deadline);

    List<Purchase> findAll();

    List<Purchase> findAllByCard(Integer idCard);

    List<Purchase> findAllByFirm(Integer idFirm);

    void deletePurchaseById(int idCard);

    Page<Purchase> findPurchaseWithFilters(
            String globalFilter,
            String username,
            String name,
            LocalDate purchaseDate,
            String dateComparisonType,
            String firmName,
            PaymentStatus status,
            Pageable pageable
    );

    Number getTotalSumToPay();
}
