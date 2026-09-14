package net.focik.homeoffice.config;

import net.focik.homeoffice.userservice.domain.port.secondary.IAppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Weryfikuje, ze {@link Config#objectMapper()} jest tym samym beanem JsonMapper, ktorego uzywa
 * Spring MVC do (de)serializacji JSON - a nie ze Spring Boot obok niego tworzy WLASNY, domyslny
 * JsonMapper (oznaczony @Primary w JacksonAutoConfiguration), ktory wygralby przy wstrzykiwaniu
 * do konwerterow HTTP i pomijal nasz niestandardowy deserializer LocalDate.
 *
 * To dokladnie ten scenariusz, ktory powodowal 500 przy PUT z data w formacie
 * "2026-01-31T00:00:00.000Z" w polu LocalDate, mimo ze Config.objectMapper() poprawnie
 * obsluguje taki format przy bezposrednim wywolaniu.
 */
class ObjectMapperWiringTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(IAppUserRepository.class, () -> mock(IAppUserRepository.class))
            .withBean(AuthenticationConfiguration.class, () -> mock(AuthenticationConfiguration.class))
            .withUserConfiguration(Config.class)
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class));

    @Test
    void shouldExposeExactlyOneJsonMapperBean_OurCustomOne() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(JsonMapper.class);

            JsonMapper jsonMapper = context.getBean(JsonMapper.class);

            // Data-czas z froncie (np. Date.toISOString()) w polu LocalDate - to dokladnie
            // format, ktory kiedys powodowal HttpMessageNotReadableException/500.
            LocalDate parsed = jsonMapper.readValue("\"2026-01-31T00:00:00.000Z\"", LocalDate.class);
            assertThat(parsed).isEqualTo(LocalDate.of(2026, 1, 31));
        });
    }

    @Test
    void shouldMapExplicitNullToZero_ForPrimitiveIntField() {
        contextRunner.run(context -> {
            JsonMapper jsonMapper = context.getBean(JsonMapper.class);

            // Front przy tworzeniu nowego obiektu wysyla jawne "id": null (jeszcze nieprzypisane) -
            // to dokladnie ten format, ktory kiedys powodowal MismatchedInputException/500
            // ("Cannot map `null` into type `int`") przy dodawaniu Label.
            TestDtoWithPrimitiveId dto = jsonMapper.readValue(
                    "{\"id\":null,\"name\":\"test\"}", TestDtoWithPrimitiveId.class);

            assertThat(dto.id).isEqualTo(0);
            assertThat(dto.name).isEqualTo("test");
        });
    }

    public static class TestDtoWithPrimitiveId {
        public int id;
        public String name;
    }
}
