package net.focik.homeoffice.fileService.domain.port.secondary;

import net.focik.homeoffice.utils.share.Module;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

public interface FileRepository {
    String downloadAndSaveImage(String imageUrl, String name, Module module);
    String saveMultipartFile(MultipartFile file, Module module);

    void deleteFile(Module module, String fileName);

    Resource getFile(Module module, String fileName);

    String saveInBucket(File file, Module module);
}
