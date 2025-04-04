package net.focik.homeoffice.finance.api.mapper;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.finance.api.dto.BankDto;
import net.focik.homeoffice.finance.domain.bank.Bank;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApiBankMapper {
    private final ModelMapper modelMapper;

    public Bank toDomain(BankDto dto) {
        return modelMapper.map(dto, Bank.class);
    }

    public BankDto toDto(Bank bank) {
        return modelMapper.map(bank, BankDto.class);
    }
}
