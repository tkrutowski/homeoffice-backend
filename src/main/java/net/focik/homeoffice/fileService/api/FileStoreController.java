package net.focik.homeoffice.fileService.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.fileService.domain.model.FileInfo;
import net.focik.homeoffice.fileService.domain.port.primary.DeleteFileUseCase;
import net.focik.homeoffice.fileService.domain.port.primary.GetFilesUseCase;
import net.focik.homeoffice.fileService.domain.port.primary.SaveFileUseCase;
import net.focik.homeoffice.utils.share.Module;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/{module}/{fileName}")
    public ResponseEntity<?> downloadFile(@PathVariable Module module, @PathVariable String fileName) throws IOException {
        log.info("Attempting to download file: {} from module: {}", fileName, module);

        Resource resource = getFilesUseCase.downloadFile(module, fileName);
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

    @DeleteMapping("/{module}/{fileName}")
    public ResponseEntity<Void> deleteFile(@PathVariable Module module, @PathVariable String fileName) {
        log.info("Deleting file: {} from module: {}", fileName, module);
        deleteFileUseCase.deleteFile(module, fileName);
        log.info("File successfully deleted: {} from module: {}", fileName, module);
        return ResponseEntity.noContent().build();
    }
}
