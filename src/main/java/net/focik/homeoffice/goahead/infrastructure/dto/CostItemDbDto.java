package net.focik.homeoffice.goahead.infrastructure.dto;

import lombok.*;
import net.focik.homeoffice.utils.share.Vat;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@ToString
@Table(name = "goahead_cost_item")
public class CostItemDbDto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long idCostItem;
    
    private String name;
    private String unit;
    private Float quantity;
    
    @Column(name = "amount_net")
    private BigDecimal amountNet; //netto
    
    @Column(name = "amount_vat")
    private BigDecimal amountVat; //vat
    
    @Column(name = "amount_gross")
    private BigDecimal amountGross; //brutto
    
    @Column(name = "vat_type")
    @Enumerated(EnumType.STRING)
    private Vat vat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cost", nullable = false)
    private CostDbDto cost;
}
