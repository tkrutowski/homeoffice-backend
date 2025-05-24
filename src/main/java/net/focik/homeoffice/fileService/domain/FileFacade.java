package net.focik.homeoffice.fileService.domain;

import lombok.RequiredArgsConstructor;
import net.focik.homeoffice.fileService.domain.model.FileInfo;
import net.focik.homeoffice.fileService.domain.port.primary.DeleteFileUseCase;
import net.focik.homeoffice.fileService.domain.port.primary.GetFilesUseCase;
import net.focik.homeoffice.fileService.domain.port.primary.SaveFileUseCase;
import net.focik.homeoffice.utils.share.Module;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class FileFacade implements SaveFileUseCase, DeleteFileUseCase, GetFilesUseCase {
    private final FileStorageService fileStorageService;

    @Override
    public FileInfo uploadFile(MultipartFile file, Module module) {
        return fileStorageService.store(file, module);
    }

    @Override
    public void deleteFile(Module module, String fileName) {
        fileStorageService.delete(module, fileName);
    }

    @Override
    public Resource downloadFile(Module module, String fileName) {
        return fileStorageService.getFile(module, fileName);
    }
}
