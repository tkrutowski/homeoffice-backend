package net.focik.homeoffice.fileService.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.fileService.domain.model.FileInfo;
import net.focik.homeoffice.fileService.domain.port.secondary.FileRepository;
import net.focik.homeoffice.utils.share.Module;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final FileRepository fileRepository;

    public FileInfo store(MultipartFile file, Module module) {
        String url = fileRepository.save(file, module);

        // Tworzenie informacji o pliku
        return new FileInfo(
                0,
                Path.of(url).getFileName().toString(),
                url,
                file.getContentType(),
                (int) file.getSize(),
                LocalDateTime.now(),
                "",
                null
        );
    }

    public void delete(Module module, String filename) {
        fileRepository.deleteFile(module, filename);
    }

    public Resource getFile(Module module, String fileName) {
        return fileRepository.getFile(module, fileName);
    }
}