package net.focik.homeoffice.goahead.infrastructure.mapper;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.goahead.domain.cost.Cost;
import net.focik.homeoffice.goahead.domain.cost.CostItem;
import net.focik.homeoffice.goahead.domain.supplier.Supplier;
import net.focik.homeoffice.goahead.infrastructure.dto.CostDbDto;
import net.focik.homeoffice.goahead.infrastructure.dto.CostItemDbDto;
import net.focik.homeoffice.goahead.infrastructure.dto.SupplierDbDto;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaCostMapper {
    private final ModelMapper modelMapper;
    public CostDbDto toDto(Cost cost) {
        return CostDbDto.builder()
                .idCost(cost.getIdCost())
                .number(cost.getNumber())
                .paymentMethod(cost.getPaymentMethod())
                .sellDate(cost.getSellDate())
                .invoiceDate(cost.getInvoiceDate())
                .paymentStatus(cost.getPaymentStatus())
                .paymentDate(cost.getPaymentDate())
                .otherInfo(cost.getOtherInfo())
                .costItems(cost.getCostItems().stream().map(item -> modelMapper.map(item, CostItemDbDto.class)).toList())
                .ksefNumber(cost.getKsefNumber())
                .invoiceHash(cost.getInvoiceHash())
                .pdfUrl(cost.getPdfUrl())
                .supplier(modelMapper.map(cost.getSupplier(), SupplierDbDto.class))
                .build();
    }

    public Cost toDomain(CostDbDto dto) {
        return Cost.builder()
                .idCost(dto.getIdCost())
                .number(dto.getNumber())
                .paymentMethod(dto.getPaymentMethod())
                .sellDate(dto.getSellDate())
                .invoiceDate(dto.getInvoiceDate())
                .paymentStatus(dto.getPaymentStatus())
                .paymentDate(dto.getPaymentDate())
                .otherInfo(dto.getOtherInfo())
                .costItems(dto.getCostItems().stream().map(item -> modelMapper.map(item, CostItem.class)).toList())
                .ksefNumber(dto.getKsefNumber())
                .invoiceHash(dto.getInvoiceHash())
                .pdfUrl(dto.getPdfUrl())
                .supplier(dto.getSupplier() == null ? null : modelMapper.map(dto.getSupplier(), Supplier.class))
                .build();
    }
}