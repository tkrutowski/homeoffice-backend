package net.focik.homeoffice.goahead.api.mapper;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.addresses.domain.Address;
import net.focik.homeoffice.goahead.api.dto.AddressDto;
import net.focik.homeoffice.goahead.api.dto.CustomerDto;
import net.focik.homeoffice.goahead.domain.customer.Customer;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApiCustomerMapper {
    private final ModelMapper mapper;

    public Customer toDomain(CustomerDto dto) {
        return Customer.builder()
                .id(dto.getId())
                .firstName(dto.getFirstName())
                .name(dto.getName())
                .regon(dto.getRegon())
                .nip(dto.getNip())
                .phone(dto.getPhone())
                .mail(dto.getMail())
                .otherInfo(dto.getOtherInfo())
                .customerType(dto.getCustomerType())
                .customerStatus(dto.getCustomerStatus())
                .address(mapper.map(dto.getAddress(), Address.class))
                .build();
    }

    public CustomerDto toDto(Customer c) {
        return CustomerDto.builder()
                .id(c.getId())
                .firstName(c.getFirstName())
                .name(c.getName())
                .nip(c.getNip() == null ? "" : c.getNip())
                .regon(c.getRegon())
                .phone(c.getPhone())
                .otherInfo(c.getOtherInfo())
                .mail(c.getMail())
                .customerStatus(c.getCustomerStatus())
                .customerType(c.getCustomerType())
                .address(mapper.map(c.getAddress(), AddressDto.class))
                .regon(c.getRegon() == null ? "" : c.getRegon())
                .build();
    }
}
