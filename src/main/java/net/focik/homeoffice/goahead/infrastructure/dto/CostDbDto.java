package net.focik.homeoffice.goahead.infrastructure.dto;

import lombok.*;
import net.focik.homeoffice.utils.share.PaymentMethod;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@ToString
@Table(name = "goahead_cost")
public class CostDbDto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer idCost;
    
    @ManyToOne
    @JoinColumn(name = "id_supplier")
    private SupplierDbDto supplier;
    
    private String number;
    private String ksefNumber;
    private String ksefUrl;
    private String pdfUrl;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate sellDate;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate invoiceDate;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate paymentDate;
    
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;
    
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;
    
    private String otherInfo;

    @OneToMany(mappedBy = "cost", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CostItemDbDto> costItems;
}
