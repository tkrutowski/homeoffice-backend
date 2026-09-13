package net.focik.homeoffice.finance.domain.loan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import net.focik.homeoffice.finance.domain.purchase.Purchase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Podglad przed konwersja wybranych Purchase na Loan (zob. {@code ConvertPurchasesToLoanUseCase}) -
 * podpowiada kwote (suma zakupow) i nazwe do wypelnienia formularza kredytu, reszte danych
 * (bank, numer, harmonogram rat) i tak wpisuje uzytkownik. {@code warnings} sa informacyjne i nie
 * blokuja konwersji - realna walidacja (ten sam user, zaden zakup juz nie powiazany/nie oplacony)
 * jest egzekwowana dopiero w {@code convertPurchasesToLoan}.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class LoanFromPurchasesDraft {
    private BigDecimal suggestedAmount;
    private String suggestedName;
    private LocalDate suggestedDate;
    private List<Purchase> purchases;
    private List<String> warnings;
}
