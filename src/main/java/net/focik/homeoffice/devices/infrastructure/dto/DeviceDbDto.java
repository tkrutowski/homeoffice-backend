package net.focik.homeoffice.devices.infrastructure.dto;

import jakarta.persistence.*;
import lombok.*;
import net.focik.homeoffice.fileService.infrastructure.dto.FileInfoDbDto;
import net.focik.homeoffice.finance.infrastructure.dto.FirmDbDto;
import net.focik.homeoffice.utils.share.ActiveStatus;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "devices")
public
class DeviceDbDto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_type")
    private DeviceTypeDbDto deviceType;

    @ManyToOne
    @JoinColumn(name = "id_firm")
    private FirmDbDto firm;
    private String name;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate purchaseDate;
    private BigDecimal purchaseAmount;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate sellDate;
    private BigDecimal sellAmount;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate warrantyEndDate;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate insuranceEndDate;
    @Column(name = "other")
    private String otherInfo;
    @Enumerated(EnumType.STRING)
    private ActiveStatus activeStatus;
    private String imageUrl;
    private String details;
    @OneToMany
    @JoinColumn(name = "owner_id")
    @SQLRestriction("module = 'DEVICE_FILES'")
    private Set<FileInfoDbDto> files = new HashSet<>();

}