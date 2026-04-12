package net.focik.homeoffice.fileService.infrastructure.storage;

import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.fileService.domain.port.secondary.FileRepository;
import net.focik.homeoffice.utils.FileHelper;
import net.focik.homeoffice.utils.share.Module;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;


@Slf4j
@Component
@Primary
public class FileStorageS3Adapter implements FileRepository {

    private final S3Client s3Client;
    private final String homeUrl;
    private final String bucketName;

    public FileStorageS3Adapter(@Autowired S3Client s3Client, @Value("${homeoffice.url}") String homeUrl, @Value("${aws.bucket.name}") String bucketName) {
        this.homeUrl = homeUrl;
        this.bucketName = bucketName;
        this.s3Client = s3Client;
    }

    @Override
    public String downloadAndSaveImage(String imageUrl, String name, Module module) {
        try {
            log.debug("Downloading file {}", imageUrl);
            URI uri = new URI(imageUrl);
            URL url = uri.toURL();

            String path = url.getPath();
            String extension = FileHelper.getFileExtension(path);

            String safeName = FileHelper.sanitizeFileName(name);
            String fileName = safeName + "_" + UUID.randomUUID() + extension;

            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(20_000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; HomeOfficeBackend/1.0)");

            long contentLength = connection.getContentLengthLong();
            String contentType = connection.getContentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = FileHelper.resolveContentType(extension);
            }

            try (InputStream inputStream = connection.getInputStream()) {
                return uploadToBucket(inputStream, contentLength, fileName, contentType, module);
            }
        } catch (IOException e) {
            log.error("Error downloading and saving file", e);
            return null;
        } catch (URISyntaxException e) {
            log.error("Error downloading and saving file", e);
            throw new RuntimeException(e);
        }
    }

    public String saveInBucket(File file, Module module) {
        String originalFileName = file.getName();
        String contentType = FileHelper.resolveContentType(FileHelper.getFileExtension(originalFileName));

        try (InputStream inputStream = new FileInputStream(file)) {
            return uploadToBucket(inputStream, file.length(), originalFileName, contentType, module);
        } catch (IOException e) {
            log.error("Error uploading file to S3", e);
            return null;
        }
    }

    @Override
    public String saveMultipartFile(MultipartFile file, Module module) {
        if (file == null || file.isEmpty()) {
            log.warn("Attempted to upload empty file");
            return null;
        }

        try {
            String originalFileName = file.getOriginalFilename();
            String baseName = originalFileName != null && originalFileName.contains(".")
                    ? originalFileName.substring(0, originalFileName.lastIndexOf('.'))
                    : originalFileName;
            String safeOriginalName = FileHelper.sanitizeFileName(baseName != null ? baseName : "file");
            String extension = FileHelper.getFileExtension(originalFileName);
            String fileName = safeOriginalName + "_" + UUID.randomUUID() + extension;

            String contentType = file.getContentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = FileHelper.resolveContentType(extension);
            }

            try (InputStream inputStream = file.getInputStream()) {
                return uploadToBucket(inputStream, file.getSize(), fileName, contentType, module);
            }
        } catch (IOException e) {
            log.error("Error uploading multipart file to S3", e);
            return null;
        } catch (S3Exception e) {
            log.error("S3 Error Code: {}", e.awsErrorDetails().errorCode());
            log.error("S3 Error Message: {}", e.awsErrorDetails().errorMessage());
            log.error("HTTP Status Code: {}", e.statusCode());
            log.error("Request ID: {}", e.awsErrorDetails().sdkHttpResponse().headers());
            throw e;
        }
    }

    @Override
    public void deleteFile(Module module, String fileName) {

    }

    @Override
    public Resource getFile(Module module, String fileName) {
        return null;
    }

    private String uploadToBucket(InputStream inputStream, long contentLength, String fileName, String contentType, Module module) {
        String directory = module.getDirectory().replaceFirst("^/+", "");
        String s3Key = directory + fileName;

        try {
            log.info("Uploading file {} to S3: {}", fileName, bucketName + s3Key);

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(contentType != null && !contentType.isBlank()
                            ? contentType
                            : "application/octet-stream")
                    .build();

            if (contentLength >= 0) {
                s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, contentLength));
            } else {
                byte[] bytes = inputStream.readAllBytes();
                s3Client.putObject(putObjectRequest, RequestBody.fromBytes(bytes));
            }

            String publicS3Url = homeUrl + module.getDirectory() + fileName;
            log.info("S3 saved file: {}", publicS3Url);
            return publicS3Url;
        } catch (IOException e) {
            log.error("Error uploading file to S3", e);
            return null;
        } catch (S3Exception e) {
            log.error("S3 Error Code: {}", e.awsErrorDetails().errorCode());
            log.error("S3 Error Message: {}", e.awsErrorDetails().errorMessage());
            log.error("HTTP Status Code: {}", e.statusCode());
            log.error("Request ID: {}", e.awsErrorDetails().sdkHttpResponse().headers());
            throw e;
        }
    }

}

