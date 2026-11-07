package net.focik.homeoffice.addresses.infrastructure.mapper;

import net.focik.homeoffice.addresses.domain.Address;
import net.focik.homeoffice.addresses.infrastructure.dto.AddressDbDto;
import org.springframework.stereotype.Component;

@Component
public class JpaMapper {

    public Address toDomain(AddressDbDto dto) {
        return Address.builder()
                .city(dto.getCity() == null ? "" : dto.getCity())
                .street(dto.getStreet() == null ? "" : dto.getStreet())
                .zip(dto.getZip() == null ? "" : dto.getZip())
                .id(dto.getId())
                .build();
    }

    public AddressDbDto toDto(Address address) {
        AddressDbDto dto = new AddressDbDto();

        dto.setId(address.getId() == 0 ? null : address.getId());
        dto.setStreet(address.getStreet() == null ? "" : address.getStreet());
        dto.setCity(address.getCity() == null ? "" : address.getCity());
        dto.setZip(address.getZip() == null ? "" : address.getZip());

        return dto;
    }
}
