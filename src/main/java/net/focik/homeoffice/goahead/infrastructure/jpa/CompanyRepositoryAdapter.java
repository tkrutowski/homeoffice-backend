package net.focik.homeoffice.goahead.infrastructure.jpa;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.goahead.domain.company.Company;
import net.focik.homeoffice.goahead.domain.company.port.secondary.CompanyRepository;
import net.focik.homeoffice.goahead.infrastructure.dto.CompanyDbDto;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Slf4j
@Repository("jpaCompanyRepository")
@RequiredArgsConstructor
public class CompanyRepositoryAdapter implements CompanyRepository {

    private final CompanyDtoRepository companyDtoRepository;
    private final ModelMapper mapper;

    @Override
    public Company save(Company company) {
        CompanyDbDto dbDto = mapper.map(company, CompanyDbDto.class);
        CompanyDbDto save = companyDtoRepository.save(dbDto);
        return mapper.map(save, Company.class);
    }

    @Override
    public Optional<Company> get() {
        return companyDtoRepository.findAll().stream().findFirst()
                .map(dbDto -> mapper.map(dbDto, Company.class));
    }

    @Override
    public Optional<Company> findById(String companyName) {
        return companyDtoRepository.findById(companyName)
                .map(dbDto -> mapper.map(dbDto, Company.class));
    }
}
