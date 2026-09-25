package net.focik.homeoffice.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import lombok.Setter;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Wysyla logi do S3 partiami jako obiekty {@code <keyPrefix>homeoffice-<data>-<epoch>-<instance>.log}
 * (klucze parsuje {@code S3LogsRepositoryAdapter}).
 * <p>
 * Zapis do S3 wykonuje wylacznie watek schedulera (co {@code flushIntervalSeconds} albo po zebraniu
 * {@code batchSize} wpisow), wiec {@link #append} nigdy nie czeka na siec. Partia, ktorej nie uda sie wyslac,
 * wraca na poczatek bufora i jest ponawiana przy nastepnym flushu.
 */
@Setter
public class S3LogAppender extends AppenderBase<ILoggingEvent> {

    // Gorny limit bufora trzymanego mimo nieudanych uploadow (znaki); po przekroczeniu odrzucane sa najstarsze wpisy
    private static final int MAX_BUFFERED_CHARS = 5_000_000;

    private String bucketName;
    private String keyPrefix = "logs/";
    private String awsRegion = "eu-central-1";
    // nazwa instancji (np. ec2, synology, local) dopisywana do klucza obiektu - rozroznia logi z wielu instancji
    private String instance = "unknown";
    private int batchSize = 100;
    private int flushIntervalSeconds = 60;

    private LayoutWrappingEncoder<ILoggingEvent> encoder;
    private S3Client s3Client;
    private ScheduledExecutorService scheduler;

    private final StringBuilder logBuffer = new StringBuilder();
    private final AtomicBoolean flushQueued = new AtomicBoolean(false);
    private int eventCount = 0;

    @Override
    public void start() {
        if (bucketName == null || bucketName.isEmpty()) {
            addError("Bucket name is required");
            return;
        }

        if (encoder == null) {
            addError("Encoder is required");
            return;
        }

        try {
            if (s3Client == null) {
                String envProfile = System.getenv("AWS_PROFILE");
                AwsCredentialsProvider provider = (envProfile != null && !envProfile.isBlank())
                        ? ProfileCredentialsProvider.builder().profileName(envProfile).build()
                        : DefaultCredentialsProvider.create();

                s3Client = S3Client.builder()
                        .region(Region.of(awsRegion))
                        .credentialsProvider(provider)
                        .build();
                addInfo("S3LogAppender uses " + (envProfile != null && !envProfile.isBlank()
                        ? "AWS profile " + envProfile : "DefaultCredentialsProvider"));
            }

            encoder.start();

            scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "s3-log-appender");
                thread.setDaemon(true);
                return thread;
            });
            scheduler.scheduleAtFixedRate(this::flush, flushIntervalSeconds, flushIntervalSeconds, TimeUnit.SECONDS);

            addInfo("S3LogAppender started: bucket=" + bucketName + ", region=" + awsRegion + ", instance=" + safeInstance()
                    + ", batchSize=" + batchSize + ", flushIntervalSeconds=" + flushIntervalSeconds);
            super.start();
        } catch (Exception e) {
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

            boolean batchFull;
            synchronized (logBuffer) {
                logBuffer.append(formattedMessage);
                eventCount++;
                batchFull = eventCount >= batchSize;
            }

            if (batchFull && flushQueued.compareAndSet(false, true)) {
                scheduler.execute(this::flush);
            }
        } catch (Exception e) {
            addError("Failed to encode log event", e);
        }
    }

    private synchronized void flush() {
        flushQueued.set(false);

        String content;
        synchronized (logBuffer) {
            if (logBuffer.length() == 0) {
                return;
            }
            content = logBuffer.toString();
            logBuffer.setLength(0);
            eventCount = 0;
        }

        try {
            upload(content);
        } catch (Exception e) {
            requeue(content);
            addError("Failed to upload logs to S3, will retry on next flush: " + e.getMessage(), e);
        }
    }

    private void upload(String content) {
        String date = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String timestamp = String.valueOf(System.currentTimeMillis());
        String key = keyPrefix + "homeoffice-" + date + "-" + timestamp + "-" + safeInstance() + ".log";

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("text/plain; charset=utf-8")
                .build();

        s3Client.putObject(putRequest, RequestBody.fromBytes(content.getBytes(StandardCharsets.UTF_8)));
    }

    // Wraca nieudana partie na poczatek bufora. eventCount zostaje 0, zeby nie ponawiac wysylki przy kazdym wpisie
    // (kolejna proba przy najblizszym flushu wg harmonogramu).
    private void requeue(String content) {
        synchronized (logBuffer) {
            logBuffer.insert(0, content);
            int overflow = logBuffer.length() - MAX_BUFFERED_CHARS;
            if (overflow > 0) {
                logBuffer.delete(0, overflow);
                addWarn("S3 log buffer exceeded " + MAX_BUFFERED_CHARS + " chars, dropped the oldest " + overflow);
            }
        }
    }

    // Klucz S3 jest parsowany przez S3LogsRepositoryAdapter - dozwolone tylko [A-Za-z0-9_.-]
    private String safeInstance() {
        String sanitized = instance == null ? "" : instance.trim().replaceAll("[^A-Za-z0-9_.-]", "_");
        return sanitized.isEmpty() ? "unknown" : sanitized;
    }

    @Override
    public void stop() {
        if (scheduler != null) {
            // najpierw dokanczamy zaplanowane flushe, potem ostatni flush pozostalego bufora
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            flush();
        }

        if (encoder != null) {
            encoder.stop();
        }

        if (s3Client != null) {
            s3Client.close();
        }

        super.stop();
    }
}
