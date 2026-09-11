package net.focik.homeoffice.finance.domain.loanproposal;

import net.focik.homeoffice.finance.domain.bank.Bank;
import net.focik.homeoffice.finance.domain.bank.port.primary.GetBankUseCase;
import net.focik.homeoffice.finance.domain.loanproposal.port.secondary.LoanExtractorPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanProposalExtractionServiceTest {

    @Mock
    private LoanExtractorPort loanExtractorPort;

    @Mock
    private GetBankUseCase getBankUseCase;

    private LoanProposalExtractionService service;

    @BeforeEach
    void setUp() {
        service = new LoanProposalExtractionService(loanExtractorPort, getBankUseCase);
    }

    @Test
    void extract_ShouldReturnEmpty_WhenEmailIsNotRecognizedAsLoanDocument() {
        when(loanExtractorPort.extract("newsletter")).thenReturn(
                LoanExtractionResult.builder().isLoanDocument(false).build());

        ExtractedProposals result = service.extract("newsletter");

        assertThat(result.isEmpty()).isTrue();
        assertThat(result.loan()).isEmpty();
        assertThat(result.purchase()).isEmpty();
    }

    @Test
    void extract_ShouldMapFieldsAndResolveBankByName_WhenBankMatchesExistingOne() {
        when(loanExtractorPort.extract("mail")).thenReturn(LoanExtractionResult.builder()
                .isLoanDocument(true)
                .bankOrCreditor("PayPo")
                .amount("1 234,56")
                .installmentAmount("123.45")
                .numberOfInstallments(10)
                .firstPaymentDate("2026-10-01")
                .loanCost("50.00")
                .otherInfo("raty 0%")
                .build());
        when(getBankUseCase.findByAll()).thenReturn(List.of(
                bank(1, "mBank"),
                bank(2, "PayPo Sp. z o.o.")
        ));

        ExtractedProposals result = service.extract("mail");

        assertThat(result.loan()).isPresent();
        ProposedLoanData data = result.loan().get();
        assertThat(data.getBankId()).isEqualTo(2);
        assertThat(data.getBankName()).isEqualTo("PayPo");
        assertThat(data.getAmount()).isEqualByComparingTo(new BigDecimal("1234.56"));
        assertThat(data.getInstallmentAmount()).isEqualByComparingTo(new BigDecimal("123.45"));
        assertThat(data.getNumberOfInstallments()).isEqualTo(10);
        assertThat(data.getFirstPaymentDate()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(data.getLoanCost()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(data.getOtherInfo()).isEqualTo("raty 0%");
    }

    @Test
    void extract_ShouldLeaveBankIdNull_WhenNoExistingBankMatchesName() {
        when(loanExtractorPort.extract("mail")).thenReturn(LoanExtractionResult.builder()
                .isLoanDocument(true)
                .bankOrCreditor("Nieznany Wierzyciel")
                .build());
        when(getBankUseCase.findByAll()).thenReturn(List.of(bank(1, "mBank")));

        ExtractedProposals result = service.extract("mail");

        assertThat(result.loan()).isPresent();
        assertThat(result.loan().get().getBankId()).isNull();
        assertThat(result.loan().get().getBankName()).isEqualTo("Nieznany Wierzyciel");
    }

    @Test
    void extract_ShouldReturnNullAmount_WhenAmountIsUnparseable() {
        when(loanExtractorPort.extract("mail")).thenReturn(LoanExtractionResult.builder()
                .isLoanDocument(true)
                .bankOrCreditor("PayPo")
                .amount("brak danych")
                .build());
        when(getBankUseCase.findByAll()).thenReturn(List.of());

        ExtractedProposals result = service.extract("mail");

        assertThat(result.loan()).isPresent();
        assertThat(result.loan().get().getAmount()).isNull();
    }

    @Test
    void extract_ShouldAlsoProposePurchase_WhenMerchantNameIsRecognized() {
        when(loanExtractorPort.extract("mail")).thenReturn(LoanExtractionResult.builder()
                .isLoanDocument(true)
                .bankOrCreditor("PayPo")
                .merchantName("GLOBAL-E.SHELLY EU")
                .amount("299.99")
                .numberOfInstallments(4)
                .otherInfo("raty 0%")
                .build());
        when(getBankUseCase.findByAll()).thenReturn(List.of());

        ExtractedProposals result = service.extract("mail");

        assertThat(result.purchase()).isPresent();
        ProposedPurchaseData purchase = result.purchase().get();
        assertThat(purchase.getName()).isEqualTo("GLOBAL-E.SHELLY EU");
        assertThat(purchase.getAmount()).isEqualByComparingTo(new BigDecimal("299.99"));
        assertThat(purchase.getPurchaseDate()).isEqualTo(LocalDate.now());
        assertThat(purchase.getOtherInfo()).isEqualTo("raty 0%");
    }

    @Test
    void extract_ShouldNotProposePurchase_WhenMerchantNameIsMissing() {
        when(loanExtractorPort.extract("mail")).thenReturn(LoanExtractionResult.builder()
                .isLoanDocument(true)
                .bankOrCreditor("mBank")
                .amount("50000")
                .build());
        when(getBankUseCase.findByAll()).thenReturn(List.of());

        ExtractedProposals result = service.extract("mail");

        assertThat(result.loan()).isPresent();
        assertThat(result.purchase()).isEmpty();
    }

    private Bank bank(int id, String name) {
        return Bank.builder().id(id).name(name).build();
    }
}
