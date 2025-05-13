package net.focik.homeoffice.finance.infrastructure.jpa;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.finance.domain.bank.Bank;
import net.focik.homeoffice.finance.domain.bank.port.secondary.BankRepository;
import net.focik.homeoffice.finance.infrastructure.dto.BankDbDto;
import net.focik.homeoffice.finance.infrastructure.mapper.JpaBankMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class BankRepositoryAdapter implements BankRepository {

    private final BankDtoRepository bankDtoRepository;
    private final JpaBankMapper mapper;

    @Override
    public Bank save(Bank bank) {
        BankDbDto bankDbDto = mapper.toDto(bank);
        if (bankDbDto.getId() == 0){
            bankDbDto.setId(null);
        }
        BankDbDto save = bankDtoRepository.save(bankDbDto);
        return mapper.toDomain(save);
    }

    @Override
    public void delete(Integer id) {
        bankDtoRepository.deleteById(id);
    }


    @Override
    public List<Bank> findAll() {
        return bankDtoRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Bank> findById(Integer id) {
        return bankDtoRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Bank> findByName(String name) {
        return bankDtoRepository.findByName(name).map(mapper::toDomain);
    }

}
