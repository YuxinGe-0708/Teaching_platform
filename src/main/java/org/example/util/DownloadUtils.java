package org.example.util;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

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

    public static String store(MultipartFile file, String... directories) throws IOException {
        if (file == null || file.isEmpty()) return null;
        Path root = uploadRoot();
        Path directory = root;
        if (directories != null) {
            for (String value : directories) {
                if (value == null || value.trim().isEmpty()) continue;
                Path segment = Paths.get(value).normalize();
                if (segment.isAbsolute() || segment.getNameCount() != 1 || "..".equals(segment.toString())) {
                    throw new IllegalArgumentException("invalid upload directory");
                }
                directory = directory.resolve(segment);
            }
        }
        directory = directory.normalize();
        if (!directory.startsWith(root)) throw new IllegalArgumentException("upload directory is outside uploads");
        Files.createDirectories(directory);

        String original = file.getOriginalFilename();
        String filename = original == null || original.trim().isEmpty()
                ? "attachment"
                : Paths.get(original).getFileName().toString();
        Path target = directory.resolve(System.currentTimeMillis() + "_" + filename).normalize();
        if (!target.startsWith(root)) throw new IllegalArgumentException("upload target is outside uploads");
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return "uploads/" + root.relativize(target).toString().replace('\\', '/');
    }

    public static Path resolvePath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("filePath is empty");
        }
        Path root = uploadRoot();
        Path direct = Paths.get(filePath).toAbsolutePath().normalize();
        if (direct.startsWith(root)) return direct;

        String portable = filePath.trim().replace('\\', '/');
        int marker = portable.indexOf("uploads/");
        if (marker >= 0) portable = portable.substring(marker + "uploads/".length());
        Path relative = Paths.get(portable).normalize();
        if (relative.isAbsolute() || relative.startsWith("..")) {
            throw new IllegalArgumentException("filePath is outside uploads");
        }
        Path resolved = root.resolve(relative).normalize();
        if (!resolved.startsWith(root)) throw new IllegalArgumentException("filePath is outside uploads");
        return resolved;
    }

    private static Path uploadRoot() {
        String configured = System.getProperty("app.upload.path");
        if (configured == null || configured.trim().isEmpty()) configured = System.getenv("APP_UPLOAD_PATH");
        if (configured == null || configured.trim().isEmpty()) configured = "uploads";
        return Paths.get(configured).toAbsolutePath().normalize();
    }

    private static Path safeUploadPath(String filePath) {
        return resolvePath(filePath);
    }
}
