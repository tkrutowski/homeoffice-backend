package net.focik.homeoffice.fileService.infrastructure.storage;

import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.fileService.domain.port.secondary.FileRepository;
import net.focik.homeoffice.utils.share.Module;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Component
public class FileStorageAdapter implements FileRepository {
    @Value("${homeoffice.directory.public}")
    private final String localCatalog;
    @Value("${homeoffice.url}")
    private final String homeUrl;

    public FileStorageAdapter(@Value("${homeoffice.directory.local}") String localCatalog, @Value("${homeoffice.url}") String homeUrl) {
        this.localCatalog = localCatalog;
        this.homeUrl = homeUrl;
    }

    @Override
    public String downloadAndSaveImage(String imageUrl, String name, Module module) {
        try {
            log.debug("Downloading image {}", imageUrl);
            URI uri = new URI(imageUrl);
            URL url = uri.toURL();

            // Pobieranie rozszerzenia pliku z URL
            String path = url.getPath();
            String extension = path.substring(path.lastIndexOf("."));

            String fileName = name.trim().replace(" ", "_") + "_" + UUID.randomUUID() + extension; // Generowanie unikalnej nazwy pliku
            File outputFile = new File(localCatalog + module.getDirectory() + "/" + fileName);
            log.debug("Saving image {} in {}", fileName, outputFile);
            // Pobierz plik z URL i zapisz go na dysku
            FileUtils.copyURLToFile(url, outputFile, 10000, 10000);
            log.debug("URL saved file: {}", homeUrl + fileName);
            return homeUrl + module.getDirectory() +fileName;
        } catch (IOException e) {
            log.error("Error downloading ans saving image (return null)",e);
            return null;
        } catch (URISyntaxException e) {
            log.error("Error downloading ans saving image",e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String saveMultipartFile(MultipartFile file, Module module) {
        log.debug("Starting file save process. Original filename: {}, module: {}", file.getOriginalFilename(), module);

        try {
            if (file.isEmpty()) {
                log.debug("Attempted to save empty file");
                throw new RuntimeException("Nie można zapisać pustego pliku.");
            }

            // Tworzenie podkatalogu dla konkretnego modułu
            Path moduleDir = Paths.get(localCatalog).resolve(module.getDirectory());
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
            Path file = Paths.get(localCatalog).resolve(module.getDirectory()).resolve(filename);
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
            Path filePath = Paths.get(localCatalog).resolve(module.getDirectory()).resolve(filename);
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

    @Override
    public String saveInBucket(File file, Module module) {
        return "";
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
        return Paths.get(Paths.get(localCatalog).toString(), module.getDirectory(), fileName);
    }
}
