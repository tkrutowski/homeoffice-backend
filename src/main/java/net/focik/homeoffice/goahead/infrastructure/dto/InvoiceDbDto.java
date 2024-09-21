package net.focik.homeoffice.goahead.infrastructure.dto;

import lombok.*;
import net.focik.homeoffice.utils.share.PaymentStatus;
import net.focik.homeoffice.utils.share.PaymentMethod;

import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "goahead_invoice")
public class InvoiceDbDto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer idInvoice;
    private Integer idCustomer;
    private String number;
    private BigDecimal amount;//brutto
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate sellDate;//data sprzedaży
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate invoiceDate;//data faktury
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate paymentDate;//termin zapłaty
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;
    private String otherInfo;
}
