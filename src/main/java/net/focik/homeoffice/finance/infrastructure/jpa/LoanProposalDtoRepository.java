package net.focik.homeoffice.finance.infrastructure.jpa;

import net.focik.homeoffice.finance.domain.loanproposal.LoanProposalStatus;
import net.focik.homeoffice.finance.infrastructure.dto.LoanProposalDbDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanProposalDtoRepository extends JpaRepository<LoanProposalDbDto, Integer> {

    Optional<LoanProposalDbDto> findBySourceMessageId(String sourceMessageId);

    List<LoanProposalDbDto> findAllByStatus(LoanProposalStatus status);

    /**
     * Odpina propozycje od usuwanego zakupu (FK created_purchase_id) i cofa je do statusu
     * {@code status}, żeby można je było obsłużyć ponownie.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update LoanProposalDbDto p set p.createdPurchaseId = null, p.status = :status, "
            + "p.handledAt = null, p.handledByUserId = null where p.createdPurchaseId = :purchaseId")
    int detachPurchase(@Param("purchaseId") Integer purchaseId, @Param("status") LoanProposalStatus status);
}
