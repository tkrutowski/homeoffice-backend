package net.focik.homeoffice.utils;

import lombok.extern.slf4j.Slf4j;

import java.text.Normalizer;

@Slf4j
public class FileHelper {

    public static String sanitizeFileName(String name) {
        String normalized = Normalizer.normalize(name == null ? "" : name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        String sanitized = normalized
                .trim()
                .replaceAll("\\s+", "_")
                .replaceAll("[\\\\/:*?\"<>|#%&{}$!@+=`~\\[\\]()';,]+", "_")
                .replaceAll("[^a-zA-Z0-9._-]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^[_ .-]+|[_ .-]+$", "");

        return sanitized.isBlank() ? "file" : sanitized;
    }

    public static String getFileExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int index = fileName.lastIndexOf(".");
        return index >= 0 ? fileName.substring(index) : "";
    }

    public static String resolveContentType(String extension) {
        return switch (extension.toLowerCase()) {
            case ".png" -> "image/png";
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".gif" -> "image/gif";
            case ".webp" -> "image/webp";
            case ".pdf" -> "application/pdf";
            default -> "application/octet-stream";
        };
    }
}

