package net.focik.homeoffice.finance.domain.loanproposal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.finance.domain.bank.Bank;
import net.focik.homeoffice.finance.domain.bank.port.primary.GetBankUseCase;
import net.focik.homeoffice.finance.domain.card.Card;
import net.focik.homeoffice.finance.domain.card.port.primary.GetCardUseCase;
import net.focik.homeoffice.finance.domain.firm.Firm;
import net.focik.homeoffice.finance.domain.firm.port.primary.GetFirmUseCase;
import net.focik.homeoffice.finance.domain.loanproposal.port.secondary.LoanExtractorPort;
import net.focik.homeoffice.utils.share.ActiveStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * Wywołuje ekstrakcję przez Claude i mapuje wynik na {@link ProposedLoanData}, tym samym wzorcem
 * co {@code ClaudeInvoiceParserService} dla faktur: model zwraca luźno typowany tekst, tu jest on
 * parsowany na konkretne typy i (best-effort) dopasowywany do istniejących danych - tu do banku
 * po nazwie, tam do dostawcy po NIP.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoanProposalExtractionService {

    private final LoanExtractorPort loanExtractorPort;
    private final GetBankUseCase getBankUseCase;
    private final GetCardUseCase getCardUseCase;
    private final GetFirmUseCase getFirmUseCase;

    /**
     * @param subject temat maila (nie treść) - jedyne miejsce, gdzie zwykle pojawia się nazwa
     *                metody płatności (np. "Potwierdzenie płatności kartą Allegro Pay"), więc
     *                służy do dopasowania karty i (best-effort) firmy
     * @param idUser  domownik dopasowany po nadawcy przy ingest (zob.
     *                {@code LoanProposalFacade.resolveIdUserFromEmail}) - zawężenie dopasowania
     *                karty do jego kart, gdy znany; null, gdy dopasowanie nadawcy się nie powiodło
     * @return puste (oba kandydaty), gdy Claude nie rozpoznał dokumentu jako kredytu
     * (proposal ma wtedy trafić na FAILED)
     */
    public ExtractedProposals extract(String emailText, String subject, Integer idUser) {
        LoanExtractionResult result = loanExtractorPort.extract(emailText);

        if (!result.isLoanDocument()) {
            log.info("Email not recognized as a loan document");
            return ExtractedProposals.none();
        }

        ProposedLoanData loan = ProposedLoanData.builder()
                .originalSenderEmail(result.getOriginalSenderEmail())
                .bankId(resolveBankId(result.getBankOrCreditor()))
                .bankName(result.getBankOrCreditor())
                .name(resolveName(result))
                .amount(parseAmount(result.getAmount()))
                .date(LocalDate.now())
                .loanNumber(result.getLoanNumber())
                .accountNumber(result.getAccountNumber())
                .installmentAmount(parseAmount(result.getInstallmentAmount()))
                .numberOfInstallments(result.getNumberOfInstallments())
                .firstPaymentDate(parseDate(result.getFirstPaymentDate()))
                .loanCost(parseAmount(result.getLoanCost()))
                .otherInfo(result.getOtherInfo())
                .build();

        return ExtractedProposals.of(loan, buildPurchaseCandidate(result, subject, idUser).orElse(null));
    }

    /**
     * Propozycja zakupu ma sens tylko, gdy e-mail dotyczy finansowania KONKRETNEGO zakupu
     * (PayPo, Allegro - Claude wtedy wypełnia merchantName), nie zwykłego kredytu bankowego
     * (gotówkowego/hipotecznego), gdzie nie ma czego zaksięgować jako "zakup".
     */
    private Optional<ProposedPurchaseData> buildPurchaseCandidate(LoanExtractionResult result, String subject, Integer idUser) {
        if (result.getMerchantName() == null || result.getMerchantName().isBlank()) {
            return Optional.empty();
        }

        return Optional.of(ProposedPurchaseData.builder()
                .name(result.getMerchantName())
                .amount(parseAmount(result.getAmount()))
                .purchaseDate(LocalDate.now())
                .otherInfo(result.getOtherInfo())
                .idUser(idUser)
                .idCard(resolveCardId(subject, idUser))
                .idFirm(resolveFirmId(result.getMerchantName(), subject))
                .build());
    }

    /**
     * "Nazwa" propozycji ma opisywać CO zostało kupione (sklep/przedmiot), nie KTO sfinansował -
     * to już jest w bankName. Gdy Claude nie znalazł konkretnego sklepu (np. zwykły kredyt
     * gotówkowy/hipoteczny bez powiązanego zakupu), spadamy z powrotem na bankOrCreditor.
     */
    private String resolveName(LoanExtractionResult result) {
        return result.getMerchantName() != null && !result.getMerchantName().isBlank()
                ? result.getMerchantName()
                : result.getBankOrCreditor();
    }

    private Integer resolveBankId(String bankOrCreditor) {
        if (bankOrCreditor == null || bankOrCreditor.isBlank()) {
            return null;
        }
        String needle = bankOrCreditor.trim().toLowerCase(Locale.ROOT);
        List<Bank> banks = getBankUseCase.findByAll();
        return banks.stream()
                .filter(bank -> bank.getName() != null)
                .filter(bank -> {
                    String name = bank.getName().toLowerCase(Locale.ROOT);
                    return name.contains(needle) || needle.contains(name);
                })
                .findFirst()
                .map(Bank::getId)
                .orElse(null);
    }

    /**
     * Dopasowuje kartę po nazwie w tytule maila - jedyne miejsce, gdzie metoda płatności zwykle
     * się pojawia (np. "Potwierdzenie płatności kartą Allegro Pay", "Transakcja ... w PayPo.pl").
     * Zawężone do kart usera, gdy jest znany (dwoje domowników może mieć kartę o tej samej
     * nazwie), w przeciwnym razie sprawdzane są wszystkie aktywne karty.
     */
    private Integer resolveCardId(String subject, Integer idUser) {
        if (subject == null || subject.isBlank()) {
            return null;
        }
        List<Card> cards = idUser != null
                ? getCardUseCase.findByUserAndStatus(idUser, ActiveStatus.ACTIVE)
                : getCardUseCase.findByStatus(ActiveStatus.ACTIVE);
        return matchByName(subject, cards, Card::getCardName, Card::getId);
    }

    /**
     * Dopasowuje firmę najpierw po nazwie sklepu z treści maila (merchantName, np.
     * "JMP S.A. BIEDRONKA" -> firma "Biedronka"), a dopiero gdy to zawiedzie, po tytule maila
     * tak samo jak karta - zgodnie z życzeniem: karta zawsze z tytułu, firma z treści, a z tytułu
     * dopiero jako fallback.
     */
    private Integer resolveFirmId(String merchantName, String subject) {
        List<Firm> firms = getFirmUseCase.findByAll();
        Integer byMerchantName = matchByName(merchantName, firms, Firm::getName, Firm::getId);
        return byMerchantName != null ? byMerchantName : matchByName(subject, firms, Firm::getName, Firm::getId);
    }

    /**
     * Dopasowanie tekst-do-nazwy w obie strony (jak przy banku): {@code needle} to zwykle dłuższy
     * fragment (tytuł maila/nazwa sklepu), a {@code candidate.name} krótsza, znana nazwa (karty,
     * firmy) - ale sprawdzamy oba kierunki na wszelki wypadek.
     */
    private <T> Integer matchByName(String needle, List<T> candidates, Function<T, String> nameOf,
                                     ToIntFunction<T> idOf) {
        if (needle == null || needle.isBlank() || candidates == null) {
            return null;
        }
        String needleLower = needle.trim().toLowerCase(Locale.ROOT);
        return candidates.stream()
                .filter(c -> nameOf.apply(c) != null && !nameOf.apply(c).isBlank())
                .filter(c -> {
                    String name = nameOf.apply(c).trim().toLowerCase(Locale.ROOT);
                    return needleLower.contains(name) || name.contains(needleLower);
                })
                .findFirst()
                .map(idOf::applyAsInt)
                .orElse(null);
    }

    private BigDecimal parseAmount(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            String normalized = value.trim().replace(",", ".").replaceAll("[^0-9.]", "");
            return normalized.isBlank() ? null : new BigDecimal(normalized);
        } catch (Exception e) {
            log.warn("Failed to parse amount: {}", value);
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse date: {}", value);
            return null;
        }
    }
}
