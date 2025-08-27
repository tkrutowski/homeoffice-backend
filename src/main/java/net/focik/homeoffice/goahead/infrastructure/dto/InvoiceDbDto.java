package net.focik.homeoffice.goahead.infrastructure.dto;

import lombok.*;
import net.focik.homeoffice.utils.share.PaymentStatus;
import net.focik.homeoffice.utils.share.PaymentMethod;

import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@ToString
@Table(name = "goahead_invoice")
public class InvoiceDbDto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer idInvoice;
    @ManyToOne
    @JoinColumn(name = "id_customer")
    private CustomerDbDto customer;
    private String number;
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

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceItemDbDto> invoiceItems;
}
