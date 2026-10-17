package net.focik.homeoffice.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.info.BuildProperties;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class BuildInfoMetricsTest {

    private SimpleMeterRegistry bindWith(BuildProperties properties) {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        if (properties != null) {
            beanFactory.addBean("buildProperties", properties);
        }
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        new BuildInfoMetrics(beanFactory.getBeanProvider(BuildProperties.class)).bindTo(registry);
        return registry;
    }

    @Test
    @DisplayName("should expose the project version as a tag of a gauge equal to 1")
    void bindTo_ShouldExposeVersion_WhenBuildPropertiesPresent() {
        Properties props = new Properties();
        props.setProperty("version", "6.6.0");

        SimpleMeterRegistry registry = bindWith(new BuildProperties(props));

        Gauge gauge = registry.find("homeoffice.build.info").tag("version", "6.6.0").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should fall back to 'unknown' when there is no build-info (e.g. started from the IDE)")
    void bindTo_ShouldUseUnknown_WhenBuildPropertiesMissing() {
        SimpleMeterRegistry registry = bindWith(null);

        assertThat(registry.find("homeoffice.build.info").tag("version", "unknown").gauge()).isNotNull();
    }
}
