package net.focik.homeoffice.fileService.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.async.AsyncTaskStartResponse;
import net.focik.homeoffice.fileService.domain.model.FileInfo;
import net.focik.homeoffice.fileService.domain.model.UploadUrlRequest;
import net.focik.homeoffice.fileService.domain.port.primary.DeleteFileUseCase;
import net.focik.homeoffice.fileService.domain.port.primary.ExtractFromFileUseCase;
import net.focik.homeoffice.fileService.domain.port.primary.GetFilesUseCase;
import net.focik.homeoffice.fileService.domain.port.primary.SaveFileUseCase;
import net.focik.homeoffice.fileService.domain.model.ConfirmUploadRequest;
import net.focik.homeoffice.goahead.api.dto.UploadUrlResponse;
import net.focik.homeoffice.utils.share.Module;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/files")
public class FileStoreController {
    final private SaveFileUseCase saveFileUseCase;
    final private DeleteFileUseCase deleteFileUseCase;
    final private GetFilesUseCase getFilesUseCase;
    final private ExtractFromFileUseCase extractFromFileUseCase;

    @GetMapping("/{module}/{fileName:.+}")
    public ResponseEntity<?> downloadFile(@PathVariable String module, @PathVariable String fileName) throws IOException {
        log.info("Attempting to download file: module={}, fileName={}", module, fileName);

        Module moduleEnum = parseModule(module);
        Resource resource = getFilesUseCase.downloadFile(moduleEnum, fileName);
        MediaType mediaType;

        try {
            String contentType = Files.probeContentType(resource.getFile().toPath());
            mediaType = contentType != null ?
                    MediaType.parseMediaType(contentType) :
                    MediaType.APPLICATION_OCTET_STREAM;
            log.debug("Detected content type for file {}: {}", fileName, mediaType);
        } catch (IOException e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
            log.debug("Unable to determine content type for file {}. Set default type: {}", fileName, mediaType);
        }
        log.info("Returning file {} with size: {} bytes", fileName, resource.contentLength());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .contentType(mediaType)
                .contentLength(resource.contentLength())
                .body(resource);
    }

    @PostMapping(value = "/{module}", consumes = {"multipart/form-data"})
    public ResponseEntity<FileInfo> uploadFile(@RequestParam("file") MultipartFile file, @PathVariable Module module) {
        log.info("Attempting to upload file: {} to module: {}", file.getOriginalFilename(), module);
        FileInfo fileInfo = saveFileUseCase.uploadFile(file, module);
        log.info("File successfully uploaded. File info: {}", fileInfo);
        return new ResponseEntity<>(fileInfo, HttpStatus.CREATED);
    }

    @PostMapping("/upload-url")
    @PreAuthorize("hasAnyAuthority('GOAHEAD_WRITE_ALL')")
    public ResponseEntity<UploadUrlResponse> generateUploadUrl(@RequestBody UploadUrlRequest request) {
        String contentType = request.contentType();
        if (!MediaType.APPLICATION_PDF_VALUE.equals(contentType)
                && !MediaType.IMAGE_PNG_VALUE.equals(contentType)
                && !MediaType.IMAGE_JPEG_VALUE.equals(contentType)) {
            return ResponseEntity.badRequest().build();
        }
        UploadUrlResponse response = saveFileUseCase.getUploadUrl(request);
        return ResponseEntity.ok(response);
    }

        @PostMapping("/upload/confirm")
    @PreAuthorize("hasAnyAuthority('GOAHEAD_WRITE_ALL')")
    public ResponseEntity<AsyncTaskStartResponse> confirmUpload(@RequestBody ConfirmUploadRequest request) {
        log.info("Potwierdzenie uploadu: objectKey={}", request.objectKey());
        if (request.objectKey() == null || request.objectKey().isBlank()) {
            log.error("Błąd: objectKey jest pusty w ConfirmUploadRequest");
            return ResponseEntity.badRequest().build();
        }
        String jobId = extractFromFileUseCase.extractStart(request.objectKey());
        return new ResponseEntity<>(new AsyncTaskStartResponse(jobId), HttpStatus.ACCEPTED);
    }

    @DeleteMapping("/{module}/{fileName:.+}")
    @PreAuthorize("hasAnyAuthority('GOAHEAD_WRITE_ALL')")
    public ResponseEntity<Void> deleteFile(@PathVariable String module, @PathVariable String fileName) {
        log.info("Attempting to delete file: module={}, fileName={}", module, fileName);
        if (fileName == null || fileName.isBlank()) {
            log.error("Error: fileName is empty in delete request");
            return ResponseEntity.badRequest().build();
        }
        Module moduleEnum = parseModule(module);
        deleteFileUseCase.deleteFile(moduleEnum, fileName);
        log.info("File successfully deleted: module={}, fileName={}", module, fileName);
        return ResponseEntity.noContent().build();
    }

    private Module parseModule(String moduleName) {
        try {
            String enumValue = moduleName.toUpperCase().replace("-", "_");
            return Module.valueOf(enumValue);
        } catch (IllegalArgumentException e) {
            log.error("Invalid module name: {}", moduleName);
            throw new IllegalArgumentException("Invalid module: " + moduleName);
        }
    }
}
