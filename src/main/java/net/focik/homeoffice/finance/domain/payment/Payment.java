package net.focik.homeoffice.finance.domain.payment;

import lombok.*;
import net.focik.homeoffice.utils.share.PaymentStatus;
import net.focik.homeoffice.utils.share.PaymentType;

import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Payment {

    private int id;
    private int idUser;

    private String name;
    private int paymentDay;
    private List<? extends Installment> installments;
    private PaymentType paymentType;
    private PaymentStatus paymentStatus;
}