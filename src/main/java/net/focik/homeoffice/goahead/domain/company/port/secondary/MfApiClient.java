package net.focik.homeoffice.goahead.domain.company.port.secondary;

import net.focik.homeoffice.goahead.domain.company.LookupResponse;

public interface MfApiClient {
    LookupResponse lookupByNip(String nip);
}
