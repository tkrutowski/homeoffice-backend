package net.focik.homeoffice.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.utils.ksef.HttpClientBuilder;
import net.focik.homeoffice.utils.ksef.HttpClientConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.akmf.ksef.sdk.api.DefaultKsefClient;

import java.net.http.HttpClient;

@Configuration
@RequiredArgsConstructor
public class KsefClientConfig {
        private final KsefInvoiceApiProperties apiProperties;

    @Bean
    public DefaultKsefClient initDefaultKsefClient() {

        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        HttpClient apiClient = HttpClientBuilder.createHttpBuilder(new HttpClientConfig()).build();
        return new DefaultKsefClient(
                apiClient,
                apiProperties,
                objectMapper);
    }
}
