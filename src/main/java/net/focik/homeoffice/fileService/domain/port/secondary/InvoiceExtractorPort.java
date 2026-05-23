package net.focik.homeoffice.fileService.domain.port.secondary;

import net.focik.homeoffice.fileService.domain.model.InvoiceClaudeResponseDto;

public interface InvoiceExtractorPort {
    InvoiceClaudeResponseDto extractFromS3(String s3Key);
}
