package net.focik.homeoffice.finance.domain.loanproposal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.finance.domain.bank.Bank;
import net.focik.homeoffice.finance.domain.bank.port.primary.GetBankUseCase;
import net.focik.homeoffice.finance.domain.loanproposal.port.secondary.LoanExtractorPort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

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

    /**
     * @return puste, gdy Claude nie rozpoznał dokumentu jako kredytu (proposal ma wtedy trafić na FAILED)
     */
    public Optional<ProposedLoanData> extract(String emailText) {
        LoanExtractionResult result = loanExtractorPort.extract(emailText);

        if (!result.isLoanDocument()) {
            log.info("Email not recognized as a loan document");
            return Optional.empty();
        }

        return Optional.of(ProposedLoanData.builder()
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
