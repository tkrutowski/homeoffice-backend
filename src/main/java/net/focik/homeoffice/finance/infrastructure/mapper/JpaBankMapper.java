package net.focik.homeoffice.finance.infrastructure.mapper;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.finance.domain.bank.Bank;
import net.focik.homeoffice.finance.infrastructure.dto.BankDbDto;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor

public class JpaBankMapper {
    private final ModelMapper modelMapper;

    public BankDbDto toDto(Bank bank) {
        return modelMapper.map(bank, BankDbDto.class);
    }

    public Bank toDomain(BankDbDto dto) {
        return modelMapper.map(dto, Bank.class);
    }
}