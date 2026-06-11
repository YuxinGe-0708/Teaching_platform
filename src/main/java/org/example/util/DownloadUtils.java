package org.example.util;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class DownloadUtils {
    private DownloadUtils() {
    }

    public static ResponseEntity<Resource> attachment(String filePath) throws Exception {
        Path path = safeUploadPath(filePath);
        Resource resource = new UrlResource(path.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }
        String filename = displayFilename(path.getFileName().toString());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(resource);
    }

    public static ResponseEntity<Resource> inline(String filePath, MediaType mediaType) throws Exception {
        Path path = safeUploadPath(filePath);
        Resource resource = new UrlResource(path.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }
        String filename = displayFilename(path.getFileName().toString());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(filename, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(resource);
    }

    public static String displayFilename(String storedFilename) {
        if (storedFilename == null || storedFilename.trim().isEmpty()) return "download";
        return storedFilename.replaceFirst("^\\d+_", "");
    }

    private static Path safeUploadPath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("filePath is empty");
        }
        Path uploadsRoot = Paths.get("uploads").toAbsolutePath().normalize();
        Path path = Paths.get(filePath).toAbsolutePath().normalize();
        if (!path.startsWith(uploadsRoot)) {
            throw new IllegalArgumentException("filePath is outside uploads");
        }
        return path;
    }
}
