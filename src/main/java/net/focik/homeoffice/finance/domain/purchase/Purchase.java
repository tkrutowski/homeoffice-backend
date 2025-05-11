package net.focik.homeoffice.finance.domain.purchase;

import lombok.*;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Purchase {

    private Integer id;
    private int idCard;
    private int idFirm;
    private int idUser;
    private String name;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate purchaseDate;
    private BigDecimal amount;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate paymentDeadline;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate paymentDate;
    private String otherInfo;
    private PaymentStatus paymentStatus;
    private boolean isInstallment;
    private String imageUrl;

    public void changePaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
        if(paymentStatus.equals(PaymentStatus.PAID)){
            paymentDate=LocalDate.now();
        } else if (paymentStatus.equals(PaymentStatus.TO_PAY)) {
            paymentDate=LocalDate.MIN;
        }
    }
}