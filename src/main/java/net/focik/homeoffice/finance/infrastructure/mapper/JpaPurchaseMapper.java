package net.focik.homeoffice.finance.infrastructure.mapper;

import net.focik.homeoffice.finance.domain.purchase.Purchase;
import net.focik.homeoffice.finance.infrastructure.dto.PurchaseDbDto;
import org.springframework.stereotype.Component;

@Component
public class JpaPurchaseMapper {

    public PurchaseDbDto toDto(Purchase purchase) {
        return PurchaseDbDto.builder()
                .id(purchase.getId())
                .idCard(purchase.getIdCard())
                .idFirm(purchase.getIdFirm())
                .idUser(purchase.getIdUser())
                .name(purchase.getName())
                .purchaseDate(purchase.getPurchaseDate())
                .amount(purchase.getAmount())
                .paymentDeadline(purchase.getPaymentDeadline())
                .paymentDate(purchase.getPaymentDate())
                .otherInfo(purchase.getOtherInfo())
                .paymentStatus(purchase.getPaymentStatus())
                .idLoan(purchase.getIdLoan())
                .build();
    }

    public Purchase toDomain(PurchaseDbDto dto) {
        return Purchase.builder()
                .id(dto.getId())
                .idCard(dto.getIdCard())
                .idFirm(dto.getIdFirm())
                .idUser(dto.getIdUser())
                .name(dto.getName())
                .purchaseDate(dto.getPurchaseDate())
                .amount(dto.getAmount())
                .paymentDeadline(dto.getPaymentDeadline())
                .paymentDate(dto.getPaymentDate())
                .otherInfo(dto.getOtherInfo())
                .paymentStatus(dto.getPaymentStatus())
                .idLoan(dto.getIdLoan())
                .build();
    }
}