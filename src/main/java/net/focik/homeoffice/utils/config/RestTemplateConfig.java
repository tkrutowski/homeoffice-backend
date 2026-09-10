package net.focik.homeoffice.utils.config;

import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        // RestTemplateBuilder auto-configuration zostala usunieta w Spring Boot 4 -
        // budujemy builder recznie zamiast go wstrzykiwac.
        return new RestTemplateBuilder().build();
    }
}
