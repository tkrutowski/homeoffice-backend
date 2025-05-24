package net.focik.homeoffice.fileService.domain.port.primary;

import net.focik.homeoffice.fileService.domain.model.FileInfo;
import net.focik.homeoffice.utils.share.Module;
import org.springframework.web.multipart.MultipartFile;

public interface SaveFileUseCase {
    FileInfo uploadFile(MultipartFile file, Module module);
}
