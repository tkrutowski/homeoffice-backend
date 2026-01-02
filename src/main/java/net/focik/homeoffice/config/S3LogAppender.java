package net.focik.homeoffice.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import lombok.Setter;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Setter
public class S3LogAppender extends AppenderBase<ILoggingEvent> {

    private String bucketName;
    private String keyPrefix = "logs/";
    private String awsRegion = "eu-central-1";
    private int batchSize = 100;
    private int flushIntervalSeconds = 60;

    private LayoutWrappingEncoder<ILoggingEvent> encoder;
    private S3Client s3Client;
    private StringBuilder logBuffer;
    private int eventCount = 0;
    private ScheduledExecutorService scheduler;

    @Override
    public void start() {
        System.out.println("=== S3LogAppender START method called ===");

        if (bucketName == null || bucketName.isEmpty()) {
            System.err.println("ERROR: Bucket name is required");
            addError("Bucket name is required");
            return;
        }

        if (encoder == null) {
            System.err.println("ERROR: Encoder is required");
            addError("Encoder is required");
            return;
        }

        try {
            System.out.println("Creating S3 client for region: " + awsRegion);
            s3Client = S3Client.builder()
                    .region(Region.of(awsRegion))
                    .build();

            logBuffer = new StringBuilder();

            System.out.println("Starting encoder...");
            encoder.start();

            System.out.println("Starting scheduler (interval: " + flushIntervalSeconds + "s)...");
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(this::flush, flushIntervalSeconds, flushIntervalSeconds, TimeUnit.SECONDS);

            System.out.println("✅ S3LogAppender started successfully!");
            System.out.println("   Bucket: " + bucketName);
            System.out.println("   Region: " + awsRegion);
            System.out.println("   Batch size: " + batchSize);
            System.out.println("   Flush interval: " + flushIntervalSeconds + "s");

            super.start();
        } catch (Exception e) {
            System.err.println("❌ Failed to start S3LogAppender: " + e.getMessage());
            e.printStackTrace();
            addError("Failed to start S3LogAppender", e);
        }
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (!isStarted()) {
            return;
        }

        try {
            // Użyj layout z encodera do sformatowania
            Layout<ILoggingEvent> layout = encoder.getLayout();
            String formattedMessage = layout.doLayout(eventObject);

            synchronized (logBuffer) {
                logBuffer.append(formattedMessage);
                eventCount++;

                if (eventCount >= batchSize) {
                    System.out.println("📦 Buffer full (" + eventCount + " logs), flushing to S3...");
                    flush();
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to encode log event: " + e.getMessage());
            addError("Failed to encode log event", e);
        }
    }

    private void flush() {
        String content;
        int count;

        synchronized (logBuffer) {
            if (logBuffer.length() == 0) {
                return;
            }

            content = logBuffer.toString();
            count = eventCount;

            // Reset buffer
            logBuffer.setLength(0);
            eventCount = 0;
        }

        try {
            String date = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
            String timestamp = String.valueOf(System.currentTimeMillis());
            String key = keyPrefix + "smartgaz-" + date + "-" + timestamp + ".log";

            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

            System.out.println("📤 Uploading " + count + " logs (" + bytes.length + " bytes) to S3: " + key);

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType("text/plain; charset=utf-8")
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(bytes));

            System.out.println("✅ Successfully uploaded logs to S3: s3://" + bucketName + "/" + key);

        } catch (Exception e) {
            System.err.println("❌ Failed to upload logs to S3: " + e.getMessage());
            e.printStackTrace();
            addError("Failed to upload logs to S3: " + e.getMessage(), e);
        }
    }

    @Override
    public void stop() {
        System.out.println("⏹️ Stopping S3LogAppender, flushing remaining logs...");

        if (scheduler != null) {
            flush();
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (encoder != null) {
            encoder.stop();
        }

        if (s3Client != null) {
            s3Client.close();
        }

        super.stop();
        System.out.println("✅ S3LogAppender stopped");
    }
}