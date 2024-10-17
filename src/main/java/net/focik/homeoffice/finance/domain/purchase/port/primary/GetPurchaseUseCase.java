package net.focik.homeoffice.finance.domain.purchase.port.primary;

import net.focik.homeoffice.finance.domain.purchase.Purchase;
import net.focik.homeoffice.utils.share.PaymentStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface GetPurchaseUseCase {

    Purchase findById(int idPurchase);

    List<Purchase> findByUser(String userName, PaymentStatus paymentStatus, LocalDate date);
    List<Purchase> findByCard(int idCard);
    Map<LocalDate, List<Purchase>> findByUserMap(String userName, PaymentStatus paymentStatus, LocalDate date);
    Map<LocalDate, List<Purchase>> findByUserMap(Integer userId, PaymentStatus paymentStatus, LocalDate date);

    Map<LocalDate, List<Purchase>> findCurrent();
}