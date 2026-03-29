package net.focik.homeoffice.goahead.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.focik.homeoffice.goahead.domain.company.LookupResponse;
import net.focik.homeoffice.goahead.domain.company.port.secondary.MfApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Log4j2
public class MfApiClientAdapter implements MfApiClient {

    private final RestTemplate restTemplate;

    @Value("${gov.mf.api.url}")
    private String mfApiUrl;

    @Override
    public LookupResponse lookupByNip(String nip) {
        String date = LocalDate.now().toString();
        
        String url = UriComponentsBuilder.fromUriString(mfApiUrl + nip)
                .queryParam("date", date)
                .toUriString();

        log.info("Calling MF API for NIP: {} at URL: {}", nip, url);
        
        try {
            return restTemplate.getForObject(url, LookupResponse.class);
        } catch (Exception e) {
            log.error("Error while calling MF API for NIP: {}", nip, e);
            return null;
        }
    }
}
