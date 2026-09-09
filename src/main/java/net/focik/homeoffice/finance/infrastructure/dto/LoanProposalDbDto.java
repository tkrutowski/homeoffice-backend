package net.focik.homeoffice.finance.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import net.focik.homeoffice.audit.AuditableEntity;
import net.focik.homeoffice.finance.domain.loanproposal.LoanProposalStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "finance_loan_proposal")
@Getter
@Setter
@ToString
@Builder
public class LoanProposalDbDto extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(nullable = false, unique = true)
    private String sourceMessageId;
    private String sourceEmailFrom;
    private String sourceSubject;
    @Column(name = "source_file_s3_key")
    private String sourceFileS3Key;
    @Enumerated(EnumType.STRING)
    private LoanProposalStatus status;
    @Lob
    @Column(columnDefinition = "json")
    private String proposedLoanJson;
    @Lob
    @Column(columnDefinition = "json")
    private String proposedPurchaseJson;
    @Column(length = 1000)
    private String failureReason;
    private Integer createdLoanId;
    private Integer createdPurchaseId;
    private LocalDateTime handledAt;
    private Integer handledByUserId;
}
