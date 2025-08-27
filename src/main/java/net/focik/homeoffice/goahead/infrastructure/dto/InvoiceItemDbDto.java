package net.focik.homeoffice.goahead.infrastructure.dto;

import lombok.*;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@ToString
@Table(name = "goahead_invoice_item")
public class InvoiceItemDbDto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long idInvoiceItem;
    private String name;
    private String pkwiu;
    private String unit;
    private Float quantity;
    private BigDecimal amount;//brutto

    @ManyToOne
    @JoinColumn(name = "id_invoice", nullable = false) // Klucz obcy
    private InvoiceDbDto invoice;
}
