package net.focik.homeoffice.config;

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
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

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
    @Primary
    public JsonMapper objectMapper() {
        // Jackson 3: obsluga java.time jest wbudowana w jackson-databind (brak osobnego
        // JavaTimeModule) - SimpleModule wystarcza do nadpisania samego deserializera LocalDate.
        //
        // Zwracany typ MUSI byc JsonMapper (nie szerszy ObjectMapper) - JacksonAutoConfiguration
        // ze Spring Boota 4 rejestruje wlasny bean `jacksonJsonMapper()` typu JsonMapper,
        // oznaczony @Primary i @ConditionalOnMissingBean(JsonMapper.class). Ten warunek jest
        // sprawdzany po zadeklarowanym typie metody @Bean, wiec przy typie ObjectMapper (nadtyp)
        // Boot NIE wykrywalby tego beana jako "juz istniejacego JsonMappera" i tworzylby swoj
        // wlasny - bez naszego modulu LocalDate - ktory jako @Primary wygrywalby przy wstrzykiwaniu
        // do konwerterow HTTP. Efekt: JSON z data-czasem (np. z Date.toISOString() na froncie)
        // w polu LocalDate powodowal 500 mimo poprawnego deserializera tutaj.
        SimpleModule localDateModule = new SimpleModule();
        localDateModule.addDeserializer(LocalDate.class, new LocalDateDeserializer());

        return JsonMapper.builder()
                .addModule(localDateModule)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                // Wiele DTO ma prymitywne pola `int id` i front wysyla dla nich jawne `null` przy
                // tworzeniu nowego obiektu (id jeszcze nie istnieje) - zamiast rzucac blad, traktujemy
                // to jak brak wartosci (0), zgodnie z konwencja "0 = brak/nieprzypisane" juz uzywana
                // w mapperach (np. ApiLoanMapper sprawdza dto.getIdUser() == 0).
                .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
                // Jackson 3: WRITE_DATES_AS_TIMESTAMPS przeniesione z SerializationFeature do DateTimeFeature
                .configure(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .build();
    }

    @Bean
    // Zewnetrzny ksef-client SDK (DefaultKsefClient) jest zbudowany na Jackson 2, dlatego
    // dla CustomKsefClient potrzebny jest osobny bean w starym typie com.fasterxml.jackson.
    // Spring rozroznia go od objectMapper() po typie (tools.jackson vs com.fasterxml.jackson).
    public com.fasterxml.jackson.databind.ObjectMapper ksefObjectMapper() {
        return new com.fasterxml.jackson.databind.ObjectMapper();
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
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) {
        return config.getAuthenticationManager();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return userRepository::findUserByUsername;
//                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        // Security 7: konstruktor bezargumentowy + setUserDetailsService(...) zostal usuniety,
        // UserDetailsService jest teraz wymagany w konstruktorze.
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService());
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
        public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) {
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
                    // Jackson 3: wyjatki deserializacji sa unchecked - reportInputMismatch rzuca
                    // odpowiedni JacksonException z kontekstem parsowania zamiast checked IOException.
                    return ctxt.reportInputMismatch(LocalDate.class, "Cannot deserialize LocalDate from: " + value);
                }
            }
        }
    }
}
