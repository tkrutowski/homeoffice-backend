package net.focik.homeoffice.finance.infrastructure.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.config.AwsProperties;
import net.focik.homeoffice.finance.domain.loanproposal.port.secondary.LoanEmailArchivePort;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * Zapisuje surowy .eml do S3, do audytu - żeby zawsze dało się prześledzić, z jakiej dokładnie
 * wiadomości powstała dana propozycja kredytu.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class S3LoanEmailArchiveAdapter implements LoanEmailArchivePort {

    private static final String PREFIX = "homeoffice/email-inbox/loan-proposals/";

    private final S3Client s3Client;
    private final AwsProperties awsProperties;

    @Override
    public Optional<String> store(String messageId, byte[] rawEml) {
        if (rawEml == null || rawEml.length == 0) {
            return Optional.empty();
        }

        String key = PREFIX + sanitize(messageId) + ".eml";
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(awsProperties.getBucketName())
                            .key(key)
                            .contentType("message/rfc822")
                            .build(),
                    RequestBody.fromBytes(rawEml)
            );
            return Optional.of(key);
        } catch (Exception e) {
            log.error("Failed to archive raw email to S3, key={}", key, e);
            return Optional.empty();
        }
    }

    private String sanitize(String messageId) {
        if (messageId == null || messageId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return messageId.replaceAll("[^a-zA-Z0-9._-]", "_").getBytes(StandardCharsets.UTF_8).length > 200
                ? UUID.randomUUID().toString()
                : messageId.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
