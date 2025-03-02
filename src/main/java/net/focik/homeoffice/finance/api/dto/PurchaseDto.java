package net.focik.homeoffice.finance.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import net.focik.homeoffice.utils.share.PaymentStatus;

import java.time.LocalDate;

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
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Europe/Warsaw")
    private LocalDate purchaseDate;
    private String amount;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Europe/Warsaw")
    private LocalDate paymentDeadline;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Europe/Warsaw")
    private LocalDate paymentDate;
    private String otherInfo;
    private PaymentStatus paymentStatus;
    @JsonProperty("installment")
    private boolean isInstallment;
}