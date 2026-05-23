package net.focik.homeoffice.fileService.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.config.AwsProperties;
import net.focik.homeoffice.fileService.domain.model.UploadUrlRequest;
import net.focik.homeoffice.fileService.domain.model.FileInfo;
import net.focik.homeoffice.fileService.domain.port.secondary.FileRepository;
import net.focik.homeoffice.goahead.api.dto.UploadUrlResponse;
import net.focik.homeoffice.utils.share.Module;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final FileRepository fileRepository;
    private final S3Presigner s3Presigner;
    private final AwsProperties awsProperties;

    public FileInfo store(MultipartFile file, Module module) {
        String url = fileRepository.saveMultipartFile(file, module);

        // Tworzenie informacji o pliku
        return new FileInfo(
                0,
                file.getOriginalFilename(),
                url,
                file.getContentType(),
                (int) file.getSize(),
                LocalDateTime.now(),
                "",
                null
        );
    }

    public void delete(Module module, String filename) {
        fileRepository.deleteFile(module, filename);
    }

    public Resource getFile(Module module, String fileName) {
        return fileRepository.getFile(module, fileName);
    }

    public UploadUrlResponse uploadUrl(UploadUrlRequest request) {
        String uuid = UUID.randomUUID().toString();
        String directory = request.module().getDirectory().replaceFirst("^/+", "");
        String s3Key = directory + "costs-" + uuid + "-" + request.fileName();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(awsProperties.getBucketName())
                .key(s3Key)
                .contentType(request.contentType())
                .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .putObjectRequest(putObjectRequest)
                .signatureDuration(Duration.ofMinutes(awsProperties.getS3PresignedUrlExpiryMinutes()))
                .build();
        String presignedUrl = s3Presigner.presignPutObject(presignRequest).url().toString();
        return new UploadUrlResponse(presignedUrl, s3Key);
    }
}