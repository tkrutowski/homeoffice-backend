package net.focik.homeoffice.fileService.domain.port.primary;

import net.focik.homeoffice.fileService.domain.model.FileInfo;
import net.focik.homeoffice.fileService.domain.model.UploadUrlRequest;
import net.focik.homeoffice.goahead.api.dto.UploadUrlResponse;
import net.focik.homeoffice.utils.share.Module;
import org.springframework.web.multipart.MultipartFile;

public interface SaveFileUseCase {
    FileInfo uploadFile(MultipartFile file, Module module);
    UploadUrlResponse getUploadUrl(UploadUrlRequest request);
}
