package net.focik.homeoffice.finance.infrastructure.dto;

import lombok.*;
import net.focik.homeoffice.utils.share.PaymentStatus;

import jakarta.persistence.*;
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
    private LocalDate purchaseDate;
//    @Column(name = "kwota")
    private BigDecimal amount;
//    @Column(name = "termin_splaty")
    private LocalDate paymentDeadline;
//    @Column(name = "date_splaty")
    private LocalDate paymentDate;
//    @Column(name = "inne")
    private String otherInfo;
    @Enumerated(EnumType.STRING)
//    @Column(name = "czy_splacony")
    private PaymentStatus paymentStatus;
//    @Column(name = "czy_raty")
//    private Boolean isInstallment;
}
