package net.focik.homeoffice.finance.infrastructure.jpa;

import net.focik.homeoffice.finance.infrastructure.dto.PurchaseDbDto;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

interface PurchaseDtoRepository extends JpaRepository<PurchaseDbDto, Integer> {

    List<PurchaseDbDto> findAllByIdUser(Integer idUser);

    List<PurchaseDbDto> findAllByIdCard(Integer idCard);

    List<PurchaseDbDto> findAllByIdUserAndPaymentStatus(Integer idUser, PaymentStatus status);

    List<PurchaseDbDto> findAllByIdUserAndPaymentDeadline(Integer idUser, LocalDate deadline);
}
