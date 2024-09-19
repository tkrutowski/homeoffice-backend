package net.focik.homeoffice.goahead.infrastructure.mapper;

import net.focik.homeoffice.addresses.domain.Address;
import net.focik.homeoffice.goahead.domain.customer.Customer;
import net.focik.homeoffice.goahead.infrastructure.dto.CustomerDbDto;
import org.springframework.stereotype.Component;

@Component
public class JpaCustomerMapper {

    public CustomerDbDto toDto(Customer c) {
        return CustomerDbDto.builder()
                .id(c.getId())
                .firstName(c.getFirstName())
                .name(c.getName())
                .nip(c.getNip())
                .phone(c.getPhone())
                .mail(c.getMail())
                .type(c.getCustomerType())
                .otherInfo(c.getOtherInfo())
                .status(c.getCustomerStatus())
                .idAddress(c.getAddress().getId())
                .build();
    }

    public Customer toDomain(CustomerDbDto dto) {
        return Customer.builder()
                .id(dto.getId())
                .firstName(dto.getFirstName())
                .name(dto.getName())
                .nip(dto.getNip())
                .phone(dto.getPhone())
                .mail(dto.getMail())
                .customerType(dto.getType())
                .customerStatus(dto.getStatus())
                .otherInfo(dto.getOtherInfo())
                .address(Address.builder().id(dto.getIdAddress()).build())
                .build();
    }
}