package net.focik.homeoffice.utils;

import net.focik.homeoffice.utils.share.Module;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class FileHelperS3Test {

    @Value("${homeoffice.url}")
    private String homeUrl;

    @Value("${aws.bucket.name}")
    private String bucketName;

//    @Test
//    void testDownloadAndSaveImage_successfulUpload() throws IOException, URISyntaxException {
//        // Arrange
//        String imageUrl = "https://example.com/images/sample.jpg";
//        String name = "sample";
//        Module module = Module.BOOK;
//
//        S3Client mockS3Client = mock(S3Client.class);
//        FileHelperS3 fileHelperS3 = new FileHelperS3(mockS3Client, homeUrl, bucketName);
//
//        InputStream mockInputStream = new ByteArrayInputStream(new byte[0]);
//        URL mockUrl = mock(URL.class);
//        URI mockUri = mock(URI.class);
//
//        when(mockUri.toURL()).thenReturn(mockUrl);
//        when(mockUrl.getPath()).thenReturn("/images/sample.jpg");
//        when(mockUrl.openStream()).thenReturn(mockInputStream);
//        when(mockUrl.openConnection().getContentLengthLong()).thenReturn(5000L);
//        URI uri = new URI(imageUrl);
//        URL url = uri.toURL();
//
//        // Act
//        String result = fileHelperS3.downloadAndSaveImage(imageUrl, name, module);
//
//        // Assert
//        assertNotNull(result);
//        assertTrue(result.startsWith(homeUrl + module.getDirectory()));
//        verify(mockS3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
//    }

    @Test
    void testDownloadAndSaveImage_ioException() throws IOException, URISyntaxException {
        // Arrange
        String imageUrl = "https://example.com/images/sample.jpg";
        String name = "sample";
        Module module = Module.BOOK;

        S3Client mockS3Client = mock(S3Client.class);
        FileHelperS3 fileHelperS3 = new FileHelperS3(mockS3Client, homeUrl, bucketName);

        URL mockUrl = mock(URL.class);
        URI mockUri = mock(URI.class);

        when(mockUri.toURL()).thenReturn(mockUrl);
        when(mockUrl.openStream()).thenThrow(new IOException("Failed to open stream"));

        // Act
        String result = fileHelperS3.downloadAndSaveImage(imageUrl, name, module);

        // Assert
        assertNull(result);
        verify(mockS3Client, times(0)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testDownloadAndSaveImage_uriSyntaxException() {
        // Arrange
        String imageUrl = "invalid-url";
        String name = "sample";
        Module module = Module.BOOK;

        S3Client mockS3Client = mock(S3Client.class);
        FileHelperS3 fileHelperS3 = new FileHelperS3(mockS3Client, homeUrl, bucketName);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> fileHelperS3.downloadAndSaveImage(imageUrl, name, module));
        verify(mockS3Client, times(0)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }
}