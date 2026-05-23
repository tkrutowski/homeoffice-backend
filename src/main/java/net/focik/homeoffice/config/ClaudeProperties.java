package net.focik.homeoffice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "claude")
@Getter
@Setter
public class ClaudeProperties {
    private String model = "claude-haiku-4-5-20251001";
    private Integer maxTokens = 4096;
}
