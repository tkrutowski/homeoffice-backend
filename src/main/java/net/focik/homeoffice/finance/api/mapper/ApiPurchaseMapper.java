package net.focik.homeoffice.finance.api.mapper;

import net.focik.homeoffice.finance.api.dto.PurchaseDto;
import net.focik.homeoffice.finance.domain.exception.PurchaseNotValidException;
import net.focik.homeoffice.finance.domain.purchase.Purchase;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component
public class ApiPurchaseMapper {

    public Purchase toDomain(PurchaseDto dto) {
        valid(dto);
        return Purchase.builder()
                .id(dto.getId())
                .idCard(dto.getIdCard())
                .idFirm(dto.getIdFirm())
                .idUser(dto.getIdUser())
                .name(dto.getName())
                .purchaseDate(dto.getPurchaseDate())
                .amount(BigDecimal.valueOf(Double.parseDouble(dto.getAmount())))
                .paymentDeadline(dto.getPaymentDeadline())
                .paymentDate(dto.getPaymentDate())
                .otherInfo(dto.getOtherInfo())
                .paymentStatus(dto.getPaymentStatus())
                .isInstallment(dto.isInstallment())
                .build();
    }

    public PurchaseDto toDto(Purchase purchase) {
        return PurchaseDto.builder()
                .id(purchase.getId())
                .idCard(purchase.getIdCard())
                .idFirm(purchase.getIdFirm())
                .idUser(purchase.getIdUser())
                .name(purchase.getName())
                .purchaseDate(purchase.getPurchaseDate())
                .amount(String.format("%.2f", purchase.getAmount()).replace(",", "."))
                .paymentDeadline(purchase.getPaymentDeadline())
                .paymentDate(purchase.getPaymentDate())
                .otherInfo(purchase.getOtherInfo() == null ? "" : purchase.getOtherInfo())
                .paymentStatus(purchase.getPaymentStatus())
                .isInstallment(purchase.isInstallment())
                .build();
    }

    public Map<String, List<PurchaseDto>> toDto(Map<LocalDate, List<Purchase>> inputMap){
        Map<String, List<PurchaseDto>> outputMap = new TreeMap<>();

        for (Map.Entry<LocalDate, List<Purchase>> entry : inputMap.entrySet()) {
            outputMap.put(entry.getKey().toString(), entry.getValue().stream()
                    .map(this::toDto)
                    .collect(Collectors.toList()));

        }
        return outputMap;
    }

    private void valid(PurchaseDto dto) {
        if (dto.getIdUser() == 0)
            throw new PurchaseNotValidException("IdUser can't be null.");
        if (dto.getPaymentDeadline() == null)
            throw new PurchaseNotValidException("Date can't be empty.");
        if (dto.getPurchaseDate() == null)
            throw new PurchaseNotValidException("Date can't be empty.");
        if (dto.getAmount().isEmpty())
            throw new PurchaseNotValidException("Amount can't be empty.");
    }
}