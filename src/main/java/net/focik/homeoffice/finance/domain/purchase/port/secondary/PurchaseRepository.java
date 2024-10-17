package net.focik.homeoffice.finance.domain.purchase.port.secondary;

import net.focik.homeoffice.finance.domain.purchase.Purchase;
import net.focik.homeoffice.utils.share.PaymentStatus;
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

    List<Purchase> findByCard(Integer idCard);

    void deletePurchaseById(int idCard);

}
