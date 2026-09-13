package net.focik.homeoffice.finance.domain.loan;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.audit.AuditAction;
import net.focik.homeoffice.audit.AuditLog;
import net.focik.homeoffice.finance.domain.exception.LoanAmountMismatchException;
import net.focik.homeoffice.finance.domain.exception.PurchaseAlreadyLinkedToLoanException;
import net.focik.homeoffice.finance.domain.exception.PurchaseAlreadyPaidException;
import net.focik.homeoffice.finance.domain.exception.PurchaseNotFoundException;
import net.focik.homeoffice.finance.domain.exception.PurchaseNotValidException;
import net.focik.homeoffice.finance.domain.exception.PurchaseUserMismatchException;
import net.focik.homeoffice.finance.domain.loan.port.primary.ConvertPurchasesToLoanUseCase;
import net.focik.homeoffice.finance.domain.loan.port.primary.SuggestLoanFromPurchasesUseCase;
import net.focik.homeoffice.finance.domain.purchase.Purchase;
import net.focik.homeoffice.finance.domain.purchase.port.primary.GetPurchaseUseCase;
import net.focik.homeoffice.finance.domain.purchase.port.primary.UpdatePurchaseUseCase;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Zamiana jednego lub kilku istniejacych Purchase w jeden Loan (np. PayPo: 1 zakup = 1 kredyt,
 * Allegro: kilka zakupow = 1 kredyt). Purchase nie sa kasowane - zostaja w bazie jako historia
 * "co kupiono", ale dostaja {@code idLoan} i status {@code CONVERTED}, wiec przestaja byc liczone
 * jako osobne obciazenie karty (zob. {@code PurchaseService.findCurrent},
 * {@code getTotalSumToPay}). Odwrotna operacja (odlinkowanie) dzieje sie przy usuwaniu Loan -
 * zob. {@link LoanFacade#deleteLoanById}.
 * <p>
 * Zaleznie wstrzykuje {@link LoanService} (pakietowo-prywatny, uzywany bezposrednio zamiast
 * {@code AddLoanUseCase}), zeby uniknac cyklu bean'ow: {@code LoanFacade} zalezy od Purchase-owych
 * portow przy usuwaniu kredytu, wiec ta klasa nie moze zaleziec od {@code LoanFacade}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseLoanConversionFacade implements SuggestLoanFromPurchasesUseCase, ConvertPurchasesToLoanUseCase {

    private static final BigDecimal AMOUNT_TOLERANCE = new BigDecimal("0.01");

    private final LoanService loanService;
    private final GetPurchaseUseCase getPurchaseUseCase;
    private final UpdatePurchaseUseCase updatePurchaseUseCase;

    @Override
    public LoanFromPurchasesDraft suggestLoanFromPurchases(List<Integer> purchaseIds) {
        List<Purchase> purchases = fetchPurchases(purchaseIds);

        BigDecimal suggestedAmount = sumAmounts(purchases);

        String suggestedName = purchases.stream()
                .map(Purchase::getName)
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .collect(Collectors.joining("; "));

        LocalDate suggestedDate = purchases.stream()
                .map(Purchase::getPurchaseDate)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(LocalDate.now());

        return LoanFromPurchasesDraft.builder()
                .suggestedAmount(suggestedAmount)
                .suggestedName(suggestedName)
                .suggestedDate(suggestedDate)
                .purchases(purchases)
                .warnings(buildWarnings(purchases))
                .build();
    }

    @Override
    @Transactional
    @AuditLog(action = AuditAction.CREATE, entityType = "Loan")
    public Loan convertPurchasesToLoan(List<Integer> purchaseIds, Loan loanData) {
        List<Purchase> purchases = fetchPurchases(purchaseIds);

        requireSameUser(purchases, loanData);
        requireNotAlreadyLinked(purchases);
        requireNotPaid(purchases);
        requireAmountMatches(purchases, loanData);

        Loan createdLoan = loanService.saveLoan(loanData);

        purchases.forEach(purchase -> {
            purchase.setIdLoan(createdLoan.getId());
            purchase.changePaymentStatus(PaymentStatus.CONVERTED);
            updatePurchaseUseCase.updatePurchase(purchase);
        });

        log.info("Converted {} purchase(s) {} into loan id={}", purchases.size(), purchaseIds, createdLoan.getId());
        return createdLoan;
    }

    private List<Purchase> fetchPurchases(List<Integer> purchaseIds) {
        if (purchaseIds == null || purchaseIds.isEmpty()) {
            throw new PurchaseNotValidException("List of purchase ids can't be empty.");
        }

        List<Integer> distinctIds = purchaseIds.stream().distinct().toList();
        List<Purchase> purchases = getPurchaseUseCase.findAllById(distinctIds);

        if (purchases.size() != distinctIds.size()) {
            Set<Integer> foundIds = purchases.stream().map(Purchase::getId).collect(Collectors.toSet());
            Integer missingId = distinctIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .findFirst()
                    .orElseThrow();
            throw new PurchaseNotFoundException(missingId);
        }

        return purchases;
    }

    private void requireSameUser(List<Purchase> purchases, Loan loanData) {
        long distinctUsers = purchases.stream().map(Purchase::getIdUser).distinct().count();
        if (distinctUsers > 1) {
            throw new PurchaseUserMismatchException();
        }

        int purchasesUser = purchases.get(0).getIdUser();
        if (loanData.getIdUser() != 0 && loanData.getIdUser() != purchasesUser) {
            throw new PurchaseUserMismatchException();
        }
    }

    private void requireNotAlreadyLinked(List<Purchase> purchases) {
        purchases.stream()
                .filter(purchase -> purchase.getIdLoan() != null)
                .findFirst()
                .ifPresent(purchase -> {
                    throw new PurchaseAlreadyLinkedToLoanException(purchase.getId(), purchase.getIdLoan());
                });
    }

    private void requireNotPaid(List<Purchase> purchases) {
        purchases.stream()
                .filter(purchase -> purchase.getPaymentStatus() == PaymentStatus.PAID)
                .findFirst()
                .ifPresent(purchase -> {
                    throw new PurchaseAlreadyPaidException(purchase.getId());
                });
    }

    private void requireAmountMatches(List<Purchase> purchases, Loan loanData) {
        BigDecimal purchasesSum = sumAmounts(purchases);
        BigDecimal loanAmount = loanData.getAmount().getNumberStripped();

        if (purchasesSum.subtract(loanAmount).abs().compareTo(AMOUNT_TOLERANCE) > 0) {
            throw new LoanAmountMismatchException(purchasesSum, loanAmount);
        }
    }

    private BigDecimal sumAmounts(List<Purchase> purchases) {
        return purchases.stream()
                .map(Purchase::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<String> buildWarnings(List<Purchase> purchases) {
        List<String> warnings = new ArrayList<>();

        if (purchases.stream().map(Purchase::getIdUser).distinct().count() > 1) {
            warnings.add("Wybrane zakupy nalezą do różnych użytkowników - konwersja zostanie odrzucona.");
        }
        if (purchases.stream().map(Purchase::getIdCard).distinct().count() > 1) {
            warnings.add("Wybrane zakupy pochodzą z różnych kart.");
        }
        if (purchases.stream().map(Purchase::getIdFirm).distinct().count() > 1) {
            warnings.add("Wybrane zakupy są przypisane do różnych firm.");
        }
        if (purchases.stream().anyMatch(purchase -> purchase.getIdLoan() != null)) {
            warnings.add("Część wybranych zakupów jest już powiązana z innym kredytem - konwersja zostanie odrzucona.");
        }
        if (purchases.stream().anyMatch(purchase -> purchase.getPaymentStatus() == PaymentStatus.PAID)) {
            warnings.add("Część wybranych zakupów jest już opłacona - konwersja zostanie odrzucona.");
        }

        return warnings;
    }
}
