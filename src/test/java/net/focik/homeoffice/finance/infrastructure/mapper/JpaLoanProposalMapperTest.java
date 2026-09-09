package net.focik.homeoffice.finance.infrastructure.mapper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.focik.homeoffice.finance.domain.loanproposal.LoanProposal;
import net.focik.homeoffice.finance.domain.loanproposal.LoanProposalStatus;
import net.focik.homeoffice.finance.domain.loanproposal.ProposedLoanData;
import net.focik.homeoffice.finance.domain.loanproposal.ProposedPurchaseData;
import net.focik.homeoffice.finance.infrastructure.dto.LoanProposalDbDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class JpaLoanProposalMapperTest {

    private JpaLoanProposalMapper mapper;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper = new JpaLoanProposalMapper(objectMapper);
    }

    @Test
    void toDto_and_toDomain_ShouldRoundTripBothCandidates_WhenBothPresent() {
        LoanProposal proposal = LoanProposal.builder()
                .id(1)
                .sourceMessageId("msg-1")
                .status(LoanProposalStatus.EXTRACTED)
                .proposedLoan(ProposedLoanData.builder()
                        .bankName("PayPo")
                        .amount(new BigDecimal("299.99"))
                        .numberOfInstallments(4)
                        .build())
                .proposedPurchase(ProposedPurchaseData.builder()
                        .name("Sklep XYZ")
                        .amount(new BigDecimal("299.99"))
                        .purchaseDate(LocalDate.of(2026, 9, 9))
                        .installment(true)
                        .build())
                .build();

        LoanProposalDbDto dbDto = mapper.toDto(proposal);

        assertThat(dbDto.getProposedLoanJson()).isNotBlank();
        assertThat(dbDto.getProposedPurchaseJson()).isNotBlank();

        LoanProposal roundTripped = mapper.toDomain(dbDto);

        assertThat(roundTripped.getProposedLoan().getBankName()).isEqualTo("PayPo");
        assertThat(roundTripped.getProposedPurchase().getName()).isEqualTo("Sklep XYZ");
        assertThat(roundTripped.getProposedPurchase().getPurchaseDate()).isEqualTo(LocalDate.of(2026, 9, 9));
        assertThat(roundTripped.getProposedPurchase().isInstallment()).isTrue();
    }

    @Test
    void toDto_and_toDomain_ShouldLeaveProposedPurchaseNull_WhenNotApplicable() {
        LoanProposal proposal = LoanProposal.builder()
                .id(1)
                .sourceMessageId("msg-2")
                .status(LoanProposalStatus.EXTRACTED)
                .proposedLoan(ProposedLoanData.builder().bankName("mBank").build())
                .proposedPurchase(null)
                .build();

        LoanProposalDbDto dbDto = mapper.toDto(proposal);
        assertThat(dbDto.getProposedPurchaseJson()).isNull();

        LoanProposal roundTripped = mapper.toDomain(dbDto);
        assertThat(roundTripped.getProposedPurchase()).isNull();
    }

    @Test
    void toDto_ShouldMapCreatedPurchaseId() {
        LoanProposal proposal = LoanProposal.builder()
                .id(1)
                .sourceMessageId("msg-3")
                .status(LoanProposalStatus.ACCEPTED)
                .createdPurchaseId(55)
                .build();

        LoanProposalDbDto dbDto = mapper.toDto(proposal);
        assertThat(dbDto.getCreatedPurchaseId()).isEqualTo(55);
        assertThat(dbDto.getCreatedLoanId()).isNull();

        LoanProposal roundTripped = mapper.toDomain(dbDto);
        assertThat(roundTripped.getCreatedPurchaseId()).isEqualTo(55);
    }
}
