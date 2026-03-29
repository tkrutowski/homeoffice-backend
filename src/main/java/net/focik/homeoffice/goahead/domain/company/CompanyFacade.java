package net.focik.homeoffice.goahead.domain.company;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.goahead.domain.company.port.primary.GetCompanyUseCase;
import net.focik.homeoffice.goahead.domain.company.port.primary.LookupCompanyUseCase;
import net.focik.homeoffice.goahead.domain.company.port.primary.UpdateCompanyUseCase;
import net.focik.homeoffice.goahead.domain.company.port.secondary.CompanyRepository;
import net.focik.homeoffice.goahead.domain.company.port.secondary.MfApiClient;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@AllArgsConstructor
public class CompanyFacade implements GetCompanyUseCase, UpdateCompanyUseCase, LookupCompanyUseCase {

    private final CompanyRepository companyRepository;
    private final MfApiClient mfApiClient;

    @Override
    public Company get() {
        Optional<Company> company = companyRepository.get();
        return company.orElse(null);
    }

    @Override
    public Company updateCompany(Company company) {
        return companyRepository.save(company);
    }

    @Override
    public LookupResponse lookupByNip(String nip) {
        return mfApiClient.lookupByNip(nip);
    }
}
