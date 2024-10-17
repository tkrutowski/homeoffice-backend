package net.focik.homeoffice.finance.infrastructure.dto;

import jakarta.persistence.*;
import lombok.*;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "Finance_Purchase")
@Getter
@ToString
@Builder
public class PurchaseDbDto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "id_zakupu")
    private Integer id;
    //    @Column(name = "id_karty")
    private Integer idCard;
    //    @Column(name = "id_firmy")
    private Integer idFirm;
    //    @Column(name = "id_uzytkownika")
    private Integer idUser;
    //    @Column(name = "towar")
    private String name;
    //    @Column(name = "data_zakupu")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate purchaseDate;
    //    @Column(name = "kwota")
    private BigDecimal amount;
    //    @Column(name = "termin_splaty")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate paymentDeadline;
    //    @Column(name = "date_splaty")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate paymentDate;
    //    @Column(name = "inne")
    private String otherInfo;
    @Enumerated(EnumType.STRING)
//    @Column(name = "czy_splacony")
    private PaymentStatus paymentStatus;
//    @Column(name = "czy_raty")
//    private Boolean isInstallment;
}
