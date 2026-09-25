package net.focik.homeoffice.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class S3LogAppenderTest {

    private LoggerContext context;
    private Logger logger;
    private S3Client s3Client;
    private S3LogAppender appender;

    @BeforeEach
    void setUp() {
        context = new LoggerContext();
        logger = context.getLogger("test");
        s3Client = mock(S3Client.class);

        PatternLayout layout = new PatternLayout();
        layout.setContext(context);
        layout.setPattern("%msg%n");
        layout.start();
        LayoutWrappingEncoder<ch.qos.logback.classic.spi.ILoggingEvent> encoder = new LayoutWrappingEncoder<>();
        encoder.setContext(context);
        encoder.setLayout(layout);

        appender = new S3LogAppender();
        appender.setContext(context);
        appender.setBucketName("test-bucket");
        appender.setInstance("ec2");
        appender.setBatchSize(2);
        appender.setFlushIntervalSeconds(3600);
        appender.setEncoder(encoder);
        appender.setS3Client(s3Client);
        appender.start();
    }

    @AfterEach
    void tearDown() {
        appender.stop();
    }

    private void log(String message) {
        appender.doAppend(new LoggingEvent(Logger.class.getName(), logger, Level.INFO, message, null, null));
    }

    private static String body(RequestBody requestBody) throws IOException {
        return new String(requestBody.contentStreamProvider().newStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    @Test
    void append_uploadsOnSchedulerThreadNotOnCallerThread() {
        List<String> uploadThreads = new CopyOnWriteArrayList<>();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenAnswer(inv -> {
            uploadThreads.add(Thread.currentThread().getName());
            return null;
        });

        log("a");
        log("b");

        verify(s3Client, timeout(3000)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        assertEquals(List.of("s3-log-appender"), uploadThreads);
    }

    @Test
    void upload_keyContainsInstanceAndBodyHasFormattedEvents() throws IOException {
        log("a");
        log("b");

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> body = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3Client, timeout(3000)).putObject(request.capture(), body.capture());

        assertTrue(request.getValue().key().matches("logs/homeoffice-\\d{4}-\\d{2}-\\d{2}-\\d+-ec2\\.log"),
                request.getValue().key());
        assertEquals("test-bucket", request.getValue().bucket());
        assertEquals("a\nb\n", body(body.getValue()).replace("\r\n", "\n"));
    }

    @Test
    void failedUpload_isRequeuedAndSentWithNextFlushWithoutLosingEvents() throws IOException {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("Access Denied").statusCode(403).build())
                .thenReturn(null);

        log("a");
        log("b");
        verify(s3Client, timeout(3000).times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        log("c");
        appender.stop();

        ArgumentCaptor<RequestBody> bodies = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3Client, times(2)).putObject(any(PutObjectRequest.class), bodies.capture());
        List<String> sent = new ArrayList<>();
        for (RequestBody requestBody : bodies.getAllValues()) {
            sent.add(body(requestBody).replace("\r\n", "\n"));
        }
        assertEquals("a\nb\n", sent.get(0));
        assertEquals("a\nb\nc\n", sent.get(1));
    }

    @Test
    void stop_flushesRemainingBuffer() throws IOException {
        log("only one");

        appender.stop();

        ArgumentCaptor<RequestBody> body = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3Client).putObject(any(PutObjectRequest.class), body.capture());
        assertEquals("only one\n", body(body.getValue()).replace("\r\n", "\n"));
    }

    @Test
    void invalidInstanceCharactersAreSanitizedInKey() {
        appender.stop();
        appender.setInstance("my host/1");
        appender.start();
        log("x");
        appender.stop();

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(request.capture(), any(RequestBody.class));
        assertTrue(request.getValue().key().endsWith("-my_host_1.log"), request.getValue().key());
    }
}
