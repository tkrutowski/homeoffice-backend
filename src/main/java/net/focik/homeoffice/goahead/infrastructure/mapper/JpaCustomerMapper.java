package net.focik.homeoffice.goahead.infrastructure.mapper;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.goahead.domain.customer.Customer;
import net.focik.homeoffice.goahead.infrastructure.dto.CustomerDbDto;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaCustomerMapper {
    private final ModelMapper modelMapper;
    public CustomerDbDto toDto(Customer c) {
        return modelMapper.map(c, CustomerDbDto.class);
    }

    public Customer toDomain(CustomerDbDto dto) {
        return modelMapper.map(dto, Customer.class);
    }
}