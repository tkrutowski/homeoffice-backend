package net.focik.homeoffice.finance.infrastructure.jpa;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.finance.domain.firm.Firm;
import net.focik.homeoffice.finance.domain.firm.port.secondary.FirmRepository;
import net.focik.homeoffice.finance.infrastructure.dto.FirmDbDto;
import net.focik.homeoffice.finance.infrastructure.mapper.JpaFirmMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class FirmRepositoryAdapter implements FirmRepository {

    private final FirmDtoRepository firmDtoRepository;
    private final JpaFirmMapper mapper;

    @Override
    public Firm save(Firm firm) {
        FirmDbDto firmDbDto = mapper.toDto(firm);
        if (firmDbDto.getId() == 0){
            firmDbDto.setId(null);
        }
        if (firmDbDto.getAddress().getId() == 0){
            firmDbDto.getAddress().setId(null);
        }
        FirmDbDto save = firmDtoRepository.save(firmDbDto);
        return mapper.toDomain(save);
    }

    @Override
    public void delete(Integer id) {
        firmDtoRepository.deleteById(id);
    }


    @Override
    public List<Firm> findAll() {
        return firmDtoRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Firm> findById(Integer id) {
        return firmDtoRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Firm> findByName(String name) {
        return firmDtoRepository.findByName(name).map(mapper::toDomain);
    }

}
