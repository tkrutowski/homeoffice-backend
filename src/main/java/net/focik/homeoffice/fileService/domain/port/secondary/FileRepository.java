package net.focik.homeoffice.fileService.domain.port.secondary;

import net.focik.homeoffice.utils.share.Module;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileRepository {
    String save(MultipartFile file, Module module);

    void deleteFile(Module module, String fileName);

    Resource getFile(Module module, String fileName);
}
