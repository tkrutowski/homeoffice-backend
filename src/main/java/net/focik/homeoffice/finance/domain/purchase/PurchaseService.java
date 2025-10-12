package net.focik.homeoffice.finance.domain.purchase;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import net.focik.homeoffice.finance.domain.exception.CardNotValidException;
import net.focik.homeoffice.finance.domain.exception.PurchaseNotFoundException;
import net.focik.homeoffice.finance.domain.exception.PurchaseNotValidException;
import net.focik.homeoffice.finance.domain.purchase.port.secondary.PurchaseRepository;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
class PurchaseService {

    private final PurchaseRepository purchaseRepository;

    Purchase addPurchase(Purchase purchase) {
        if (isNotValid(purchase))
            throw new CardNotValidException();
        return purchaseRepository.savePurchase(purchase);
    }

    List<Purchase> findPurchasesByUser(int idUser, PaymentStatus paymentStatus, LocalDate date) {
        List<Purchase> purchaseByUserId = purchaseRepository.findPurchaseByUserId(idUser);

        if (paymentStatus == null)
            return purchaseByUserId;

        purchaseByUserId = purchaseByUserId.stream()
                .filter(card -> card.getPaymentStatus().equals(paymentStatus))
                .collect(Collectors.toList());

        return purchaseByUserId;
    }

    public List<Purchase> findCurrent(int idUser) {
        Set<LocalDate> deadlines = purchaseRepository.findPurchaseByUserAndStatus(idUser, PaymentStatus.TO_PAY).stream()
//                .filter(purchase -> purchase.getPaymentStatus().equals(PaymentStatus.TO_PAY))
                .map(Purchase::getPaymentDeadline)
                .collect(Collectors.toSet());

        List<Purchase> list = new ArrayList<>();
        deadlines.forEach(date -> list.addAll(purchaseRepository.findPurchaseByUserIdAndDeadline(idUser, date)
                .stream()
                .filter(purchase -> purchase.getPaymentStatus().equals(PaymentStatus.TO_PAY))
                .toList()));
        return list;
    }

    Map<LocalDate, List<Purchase>> convertToMapByDeadline(List<Purchase> purchaseList) {
        return purchaseList.stream()
                .sorted(Comparator.comparing(Purchase::getPurchaseDate))
                .collect(Collectors.groupingBy(Purchase::getPaymentDeadline));
    }

    Purchase findPurchaseById(int idPurchase) {
        Optional<Purchase> purchaseById = purchaseRepository.findPurchaseById(idPurchase);

        if (purchaseById.isEmpty()) {
            throw new PurchaseNotFoundException(idPurchase);
        }

        return purchaseById.get();
    }

    @Transactional
    public void deletePurchase(int idPurchase) {
        purchaseRepository.deletePurchaseById(idPurchase);
    }

    public Purchase updatePurchase(Purchase purchase) {
        if (isNotValid(purchase))
            throw new PurchaseNotValidException();
        return purchaseRepository.savePurchase(purchase);
    }

    private boolean isNotValid(Purchase purchase) {
        if (Objects.equals(purchase.getAmount(), BigDecimal.ZERO))
            return true;
        return purchase.getPurchaseDate() == null && purchase.getPaymentDeadline() == null && purchase.getPaymentDate() == null;
    }

    public List<Purchase> findByCard(int idCard) {
        return purchaseRepository.findAllByCard(idCard);
    }

    public List<Purchase> getPurchasesByFirm(Integer idFirm) {
        return purchaseRepository.findAllByFirm(idFirm);
    }

    public Page<Purchase> findPurchasesPageableWithFilters(int page, int size, String sortField, String sortDirection, String globalFilter, String username, String name, LocalDate purchaseDate, String dateComparisonType, PaymentStatus status, Integer idFirm, Integer idCard) {
        String jpaField = switch (sortField) {
            case "users" -> "user.username";
            case "firms" -> "firm.name";
            case "cards" -> "card.name";
            default -> sortField.isEmpty() || "null".equals(sortField) ? "id" : sortField;
        };

        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, jpaField));

        return purchaseRepository.findPurchaseWithFilters(globalFilter, username, name, purchaseDate, dateComparisonType, status, idFirm, idCard, pageable);
    }

    public Number getTotalSumToPay() {
        return purchaseRepository.getTotalSumToPay();
    }
}