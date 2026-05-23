package net.focik.homeoffice.fileService.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.async.AsyncTask;
import net.focik.homeoffice.async.AsyncTaskService;
import net.focik.homeoffice.async.AsyncTaskStatus;
import net.focik.homeoffice.config.AwsProperties;
import net.focik.homeoffice.fileService.domain.model.InvoiceClaudeResponseDto;
import net.focik.homeoffice.fileService.domain.port.secondary.InvoiceExtractorPort;
import net.focik.homeoffice.goahead.api.dto.CostDto;
import net.focik.homeoffice.goahead.api.mapper.ApiCostMapper;
import net.focik.homeoffice.goahead.domain.cost.Cost;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaudeAnalysisService {
    private final InvoiceExtractorPort invoiceExtractorPort;
    private final ClaudeInvoiceParserService claudeInvoiceParserService;
    private final AsyncTaskService asyncTaskService;
    private final ApiCostMapper costMapper;
    private final ObjectMapper objectMapper;
    private final AwsProperties awsProperties;

    @Async
    public void startAnalysisAsync(String jobId, String s3Key) {
        AsyncTask task = asyncTaskService.getJobStatus(jobId);
        if (task == null) {
            log.error("AsyncTask not found for jobId: {}", jobId);
            return;
        }

        try {
            if (s3Key == null || s3Key.isBlank()) {
                throw new IllegalArgumentException("S3 key cannot be null or empty");
            }

            asyncTaskService.updateTaskStatus(jobId, AsyncTaskStatus.RUNNING);
            log.info("Starting Claude invoice analysis for jobId: {} from S3 key: {}", jobId, s3Key);

            InvoiceClaudeResponseDto invoiceDto = invoiceExtractorPort.extractFromS3(s3Key);

            Cost cost = claudeInvoiceParserService.parse(invoiceDto);

            CostDto costDto = costMapper.toDto(cost);

            costDto.setPdfUrl(buildS3Url(s3Key));

            String resultJson = objectMapper.writeValueAsString(costDto);
            task.setTextractResultJson(resultJson);
            asyncTaskService.updateTask(task);

            asyncTaskService.updateTaskStatus(jobId, AsyncTaskStatus.SUCCEEDED);
            log.info("Claude invoice analysis completed successfully for jobId: {}", jobId);
        } catch (Exception e) {
            log.error("Error during Claude invoice analysis for jobId: {}", jobId, e);
            task.setMessage("Error: " + e.getMessage());
            asyncTaskService.updateTask(task);
            asyncTaskService.updateTaskStatus(jobId, AsyncTaskStatus.FAILED);
        }
    }

    private String buildS3Url(String s3Key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                awsProperties.getBucketName(),
                awsProperties.getRegion(),
                s3Key);
    }
}
