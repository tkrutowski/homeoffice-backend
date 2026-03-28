package net.focik.homeoffice.goahead.infrastructure.mapper;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.goahead.domain.supplier.Supplier;
import net.focik.homeoffice.goahead.infrastructure.dto.SupplierDbDto;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaSupplierMapper {
    private final ModelMapper modelMapper;
    public SupplierDbDto toDto(Supplier s) {
        return modelMapper.map(s, SupplierDbDto.class);
    }

    public Supplier toDomain(SupplierDbDto dto) {
        return modelMapper.map(dto, Supplier.class);
    }
}