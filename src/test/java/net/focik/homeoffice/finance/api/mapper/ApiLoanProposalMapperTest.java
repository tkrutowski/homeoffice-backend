package net.focik.homeoffice.finance.api.mapper;

import net.focik.homeoffice.finance.api.dto.LoanProposalDto;
import net.focik.homeoffice.finance.domain.loanproposal.LoanProposal;
import net.focik.homeoffice.finance.domain.loanproposal.LoanProposalStatus;
import net.focik.homeoffice.finance.domain.loanproposal.ProposedLoanData;
import net.focik.homeoffice.finance.domain.loanproposal.ProposedPurchaseData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ApiLoanProposalMapperTest {

    private final ApiLoanProposalMapper mapper = new ApiLoanProposalMapper();

    @Test
    @DisplayName("should map both the proposed loan and proposed purchase candidates when both are present")
    void toDto_ShouldMapBothCandidates_WhenBothPresent() {
        LoanProposal proposal = LoanProposal.builder()
                .id(1)
                .status(LoanProposalStatus.EXTRACTED)
                .proposedLoan(ProposedLoanData.builder().bankName("PayPo").amount(new BigDecimal("299.99")).build())
                .proposedPurchase(ProposedPurchaseData.builder()
                        .name("Sklep XYZ")
                        .amount(new BigDecimal("299.99"))
                        .purchaseDate(LocalDate.of(2026, 9, 9))
                        .build())
                .build();

        LoanProposalDto dto = mapper.toDto(proposal);

        assertThat(dto.getProposedLoan().getBankName()).isEqualTo("PayPo");
        assertThat(dto.getProposedPurchase()).isNotNull();
        assertThat(dto.getProposedPurchase().getName()).isEqualTo("Sklep XYZ");
        assertThat(dto.getProposedPurchase().getPurchaseDate()).isEqualTo(LocalDate.of(2026, 9, 9));
    }

    @Test
    @DisplayName("should leave the proposed purchase null when it is not applicable")
    void toDto_ShouldLeaveProposedPurchaseNull_WhenNotApplicable() {
        LoanProposal proposal = LoanProposal.builder()
                .id(1)
                .status(LoanProposalStatus.EXTRACTED)
                .proposedLoan(ProposedLoanData.builder().bankName("mBank").build())
                .build();

        LoanProposalDto dto = mapper.toDto(proposal);

        assertThat(dto.getProposedPurchase()).isNull();
    }

    @Test
    @DisplayName("should map the created purchase id, leaving created loan id null")
    void toDto_ShouldMapCreatedPurchaseId() {
        LoanProposal proposal = LoanProposal.builder()
                .id(1)
                .status(LoanProposalStatus.ACCEPTED)
                .createdPurchaseId(55)
                .build();

        LoanProposalDto dto = mapper.toDto(proposal);

        assertThat(dto.getCreatedPurchaseId()).isEqualTo(55);
        assertThat(dto.getCreatedLoanId()).isNull();
    }
}
