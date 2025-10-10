package net.focik.homeoffice.finance.api.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import net.focik.homeoffice.utils.share.PaymentStatus;
import net.focik.homeoffice.utils.share.PaymentType;

import java.util.List;

@Builder
@Getter
@ToString
public class PaymentDto {
    private int id;
    private int idUser;

    private String name;
    private int paymentDay;
    private List<? extends InstallmentDto> installments;
    private PaymentType paymentType;
    private PaymentStatus paymentStatus;
}