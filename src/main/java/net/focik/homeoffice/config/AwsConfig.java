package net.focik.homeoffice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

@Slf4j
@Configuration
public class AwsConfig {

    @Bean(destroyMethod = "close")
    public S3Client s3Client(
            @Value("${aws.profile:}") String awsProfile
    ) {
        AwsCredentialsProvider provider = (awsProfile != null && !awsProfile.isBlank())
                ? ProfileCredentialsProvider.builder().profileName(awsProfile).build()
                : DefaultCredentialsProvider.create();

        return S3Client.builder()
                .region(Region.EU_CENTRAL_1)
                .credentialsProvider(provider)
                .build();
    }

    @Bean
    ApplicationRunner awsS3CredentialsChecker(
            S3Client s3Client,
            @Value("${aws.bucket.name}") String bucketName
    ) {
        return args -> {
            try {
                s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
                log.info("AWS S3 credentials OK. Bucket '{}' is reachable.", bucketName);
            } catch (Exception e) {
                log.error("AWS S3 credentials check FAILED for bucket '{}'.", bucketName, e);
                throw e;
            }
        };
    }
}