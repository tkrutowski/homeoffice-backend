package net.focik.homeoffice.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.goahead.domain.invoice.InvoiceItem;
import net.focik.homeoffice.goahead.infrastructure.dto.InvoiceItemDbDto;
import net.focik.homeoffice.userservice.domain.port.secondary.IAppUserRepository;
import net.focik.homeoffice.utils.ksef.HttpClientBuilder;
import net.focik.homeoffice.utils.ksef.HttpClientConfig;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
@RequiredArgsConstructor
class Config {
    private final IAppUserRepository userRepository;

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        modelMapper.addConverter(new MoneyToDoubleConverter());
        modelMapper.addConverter(new MoneyToBigDecimalConverter());
        modelMapper.addConverter(new BigDecimalToMoneyConverter());
        modelMapper.addConverter(new DoubleToMoneyConverter());

        Converter<InvoiceItemDbDto, InvoiceItem> invoiceItemDbDtoToDomainConverter = context -> {
            InvoiceItemDbDto source = context.getSource();
            InvoiceItem destination = context.getDestination();

            if (source != null && destination != null) {
                destination.setInvoiceId(source.getInvoice() != null && source.getInvoice().getId() != null
                    ? source.getInvoice().getId()
                    : 0);
            }
            return destination;
        };
        modelMapper.typeMap(InvoiceItemDbDto.class, InvoiceItem.class)
                   .setPostConverter(invoiceItemDbDtoToDomainConverter);

        return modelMapper;
    }

    @Bean
    public ObjectMapper objectMapper() {
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer());

        return new ObjectMapper()
                .registerModule(javaTimeModule)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Bean
    public HttpClient apiClient() {
        return  HttpClientBuilder.createHttpBuilder(new HttpClientConfig()).build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return userRepository::findUserByUsername;
//                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Custom deserializer dla LocalDate, obsługuje zarówno format ISO 8601 z czasem
     * (np. 2026-09-10T00:00:00.000Z) jak i format prostej daty (yyyy-MM-dd).
     * Dzięki temu frontend może wysyłać obydwa formaty.
     */
    private static class LocalDateDeserializer extends StdDeserializer<LocalDate> {
        public LocalDateDeserializer() {
            super(LocalDate.class);
        }

        @Override
        public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String value = p.getValueAsString();
            if (value == null || value.isEmpty()) {
                return null;
            }

            try {
                // Spróbuj parsować format ISO 8601 z czasem (np. 2026-09-10T00:00:00.000Z)
                LocalDateTime dateTime = LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME);
                return dateTime.toLocalDate();
            } catch (Exception e) {
                try {
                    // Spróbuj parsować format prostej daty (yyyy-MM-dd)
                    return LocalDate.parse(value, DateTimeFormatter.ISO_DATE);
                } catch (Exception ex) {
                    throw new IOException("Cannot deserialize LocalDate from: " + value, ex);
                }
            }
        }
    }
}
