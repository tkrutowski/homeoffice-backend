package net.focik.homeoffice.goahead.api.mapper;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.addresses.domain.Address;
import net.focik.homeoffice.goahead.api.dto.AddressDto;
import net.focik.homeoffice.goahead.api.dto.SupplierDto;
import net.focik.homeoffice.goahead.domain.supplier.Supplier;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApiSupplierMapper {
    private final ModelMapper mapper;

    public Supplier toDomain(SupplierDto dto) {
        return Supplier.builder()
                .id(dto.getId())
                .name(dto.getName())
                .nip(dto.getNip())
                .phone(dto.getPhone())
                .mail(dto.getMail())
                .otherInfo(dto.getOtherInfo())
                .customerStatus(dto.getCustomerStatus())
                .address(mapper.map(dto.getAddress(), Address.class))
                .accountNumber(dto.getAccountNumber())
                .build();
    }

    public SupplierDto toDto(Supplier s) {
        return SupplierDto.builder()
                .id(s.getId())
                .name(s.getName())
                .nip(s.getNip() == null ? "" : s.getNip())
                .phone(s.getPhone())
                .otherInfo(s.getOtherInfo())
                .mail(s.getMail())
                .customerStatus(s.getCustomerStatus())
                .address(mapper.map(s.getAddress(), AddressDto.class))
                .accountNumber(s.getAccountNumber() == null ? "" : s.getAccountNumber())
                .build();
    }
}
