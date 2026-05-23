package net.focik.homeoffice.fileService.domain;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.async.AsyncTask;
import net.focik.homeoffice.async.AsyncTaskService;
import net.focik.homeoffice.fileService.domain.model.UploadUrlRequest;
import net.focik.homeoffice.fileService.domain.model.FileInfo;
import net.focik.homeoffice.fileService.domain.port.primary.DeleteFileUseCase;
import net.focik.homeoffice.fileService.domain.port.primary.ExtractFromFileUseCase;
import net.focik.homeoffice.fileService.domain.port.primary.GetFilesUseCase;
import net.focik.homeoffice.fileService.domain.port.primary.SaveFileUseCase;
import net.focik.homeoffice.goahead.api.dto.UploadUrlResponse;
import net.focik.homeoffice.utils.share.Module;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class FileFacade implements SaveFileUseCase, DeleteFileUseCase, GetFilesUseCase, ExtractFromFileUseCase {
    private final FileStorageService fileStorageService;
    private final AsyncTaskService asyncTaskService;
    private final ClaudeAnalysisService claudeAnalysisService;

    @Override
    public FileInfo uploadFile(MultipartFile file, Module module) {
        return fileStorageService.store(file, module);
    }

    @Override
    public UploadUrlResponse getUploadUrl(UploadUrlRequest request) {
        return fileStorageService.uploadUrl(request);
    }

    @Override
    public void deleteFile(Module module, String fileName) {
        fileStorageService.delete(module, fileName);
    }

    @Override
    public Resource downloadFile(Module module, String fileName) {
        return fileStorageService.getFile(module, fileName);
    }

    @Override
    public String extractStart(String s3key) {
        if (s3key == null || s3key.isBlank()) {
            throw new IllegalArgumentException("S3 object key jest wymagany i nie może być pusty");
        }
        AsyncTask task = asyncTaskService.startJob(1, "CLAUDE_INVOICE_UPLOAD");
        String jobId = task.getJobId();
        claudeAnalysisService.startAnalysisAsync(jobId, s3key);
        return jobId;
    }
}
