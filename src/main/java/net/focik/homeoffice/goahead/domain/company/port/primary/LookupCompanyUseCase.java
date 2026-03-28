package net.focik.homeoffice.goahead.domain.company.port.primary;

import net.focik.homeoffice.goahead.domain.company.LookupResponse;

public interface LookupCompanyUseCase {
    LookupResponse lookupByNip(String nip);
}
