package net.focik.homeoffice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsConfig {

    @Bean(destroyMethod = "close")
    public S3Client s3Client() {
        // W Lambda, credentials są automatycznie z IAM roli
        return S3Client.builder()
                .region(Region.EU_CENTRAL_1)
                .build();
    }
}
