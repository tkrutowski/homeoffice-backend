package net.focik.homeoffice.finance.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@ToString
public class PurchaseDto {
    private int id;
    private int idCard;
    private int idFirm;
    private int idUser;
    private String name;
    private String purchaseDate;
    private String amount;
    private String paymentDeadline;
    private String paymentDate;
    private String otherInfo;
    private PaymentStatusDto paymentStatus;
    @JsonProperty("installment")
    private boolean isInstallment;
}