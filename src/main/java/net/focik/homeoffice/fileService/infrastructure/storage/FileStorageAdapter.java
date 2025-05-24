package net.focik.homeoffice.fileService.infrastructure.storage;

import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.fileService.domain.port.secondary.FileRepository;
import net.focik.homeoffice.utils.share.Module;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class FileStorageAdapter implements FileRepository {
    private final Path rootLocation;

    public FileStorageAdapter(@Value("${homeoffice.directory.private}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir);
    }

    @Override
    public String save(MultipartFile file, Module module) {
        log.debug("Starting file save process. Original filename: {}, module: {}", file.getOriginalFilename(), module);

        try {
            if (file.isEmpty()) {
                log.debug("Attempted to save empty file");
                throw new RuntimeException("Nie można zapisać pustego pliku.");
            }

            // Tworzenie podkatalogu dla konkretnego modułu
            Path moduleDir = rootLocation.resolve(module.getDirectory());
            createDirectoryIfNotExists(moduleDir);
            log.debug("Module directory ensured at: {}", moduleDir);


            // Generowanie unikalnej nazwy pliku
            String fileName = generateUniqueFileName(file.getOriginalFilename());
            Path destinationFile = moduleDir.resolve(fileName);
            log.debug("Generated unique filename: {}", fileName);

            // Kopiowanie pliku
            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
            log.debug("File successfully copied to: {}", destinationFile);

            // url do pliku
            String result = destinationFile.toString();
            log.debug("Returning file path: {}", result);
            return result;
        } catch (IOException e) {
            log.error("Failed to save file: {}", e.getMessage());
            throw new RuntimeException("Nie udało się zapisać pliku: " + e.getMessage());
        }
    }

    @Override
    public void deleteFile(Module module, String filename) {
        log.debug("Attempting to delete file: {} from module: {}", filename, module);
        try {
            Path file = rootLocation.resolve(module.getDirectory()).resolve(filename);
            log.debug("Resolved file path: {}", file);

            boolean deleted = Files.deleteIfExists(file);
            log.debug("File deletion result - file existed and was deleted: {}", deleted);
        } catch (IOException e) {
            log.error("Error while deleting file: {}, error: {}", filename, e.getMessage());
            throw new RuntimeException("Cannot delete file: " + filename);
        }
    }

    @Override
    public Resource getFile(Module module, String filename) {
        log.debug("Attempting to get file: {} from module: {}", filename, module);
        try {
            Path filePath = rootLocation.resolve(module.getDirectory()).resolve(filename);
            log.debug("Resolved file path: {}", filePath);

            if (!Files.exists(filePath)) {
                log.debug("File not found at path: {}", filePath);
                throw new FileNotFoundException("File not found: " + filePath);
            }

            Resource resource = new UrlResource(filePath.toUri());
            log.debug("Created URL resource from path: {}", filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                log.debug("Resource exists and is readable");
                return resource;
            } else {
                log.error("Resource exists but is not readable: {}", filename);
                throw new RuntimeException("Cannot read file: " + filename);
            }
        } catch (IOException e) {
            log.error("IO error while reading file: {}, error: {}", filename, e.getMessage());
            throw new RuntimeException("Cannot read file: " + filename);
        }
    }

    private String generateUniqueFileName(String originalFilename) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String fileName = StringUtils.stripFilenameExtension(StringUtils.getFilename(originalFilename));
        return String.format("%s_%s.%s", fileName, timestamp, extension);
    }

    private void createDirectoryIfNotExists(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("Nie można utworzyć katalogu: " + dir);
        }
    }

    private Path getFilePath(Module module, String fileName) {
        return Paths.get(rootLocation.toString(), module.getDirectory(), fileName);
    }
}
