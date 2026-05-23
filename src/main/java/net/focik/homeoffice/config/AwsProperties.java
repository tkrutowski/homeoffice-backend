package net.focik.homeoffice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "aws")
@Getter
@Setter
public class AwsProperties {
    private String region;
    private String bucketName;
    private Integer s3PresignedUrlExpiryMinutes = 10;
    private Double textractConfidenceThreshold = 80.0;
    private String secretKey;
    private String accessKeyId;
}
