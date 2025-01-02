package net.focik.homeoffice.utils;

import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.utils.share.Module;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.UUID;

@Slf4j
@Service
public class FileHelper {
    @Value("${homeoffice.directory}")
    private final String mainCatalog;
    @Value("${homeoffice.url}")
    private final String homeUrl;

    public FileHelper( @Value("${homeoffice.directory}") String mainCatalog, @Value("${homeoffice.url}") String homeUrl) {
        this.mainCatalog = mainCatalog;
        this.homeUrl = homeUrl;
    }

    public String downloadAndSaveImage(String imageUrl, String name, Module module) {
        try {
            log.debug("Downloading image {}", imageUrl);
            URI uri = new URI(imageUrl);
            URL url = uri.toURL();

            // Pobieranie rozszerzenia pliku z URL
            String path = url.getPath();
            String extension = path.substring(path.lastIndexOf("."));

            String fileName = name.trim().replace(" ", "_") + "_" + UUID.randomUUID() + extension; // Generowanie unikalnej nazwy pliku
            File outputFile = new File(mainCatalog + module.getImageDirectory() + "/" + fileName);
            log.debug("Saving image {} in {}", fileName, outputFile);
            // Pobierz plik z URL i zapisz go na dysku
            FileUtils.copyURLToFile(url, outputFile, 10000, 10000);
            log.debug("URL saved file: {}", homeUrl + fileName);
            return homeUrl + module.getImageDirectory() +fileName;
        } catch (IOException e) {
            log.error("Error downloading ans saving image (return null)",e);
            return null;
        } catch (URISyntaxException e) {
            log.error("Error downloading ans saving image",e);
            throw new RuntimeException(e);
        }
    }
}
