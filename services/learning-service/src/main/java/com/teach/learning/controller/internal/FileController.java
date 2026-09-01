package com.teach.learning.controller.internal;

import com.teach.learning.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;

@RestController
@RequestMapping("/internal/files")
public class FileController {
    private final Path root;
    public FileController(@Value("${app.upload-path:${APP_UPLOAD_PATH:uploads}}") String configured) {
        this.root = Paths.get(configured).toAbsolutePath().normalize();
    }

    @PostMapping("/resources")
    public ApiResponse<String> uploadResource(@RequestParam Long courseId, @RequestParam("file") MultipartFile file) {
        return upload(file, "resources", String.valueOf(courseId));
    }

    @GetMapping
    public ResponseEntity<Resource> download(@RequestParam String path) {
        try {
            Path resolved = resolve(path);
            if (!Files.isRegularFile(resolved)) return ResponseEntity.notFound().build();
            String name = resolved.getFileName().toString().replaceFirst("^\\d+_", "");
            MediaType type = MediaType.APPLICATION_OCTET_STREAM;
            String lower = name.toLowerCase();
            if (lower.endsWith(".mp4")) type = MediaType.valueOf("video/mp4");
            else if (lower.endsWith(".webm")) type = MediaType.valueOf("video/webm");
            else if (lower.endsWith(".pdf")) type = MediaType.APPLICATION_PDF;
            return ResponseEntity.ok().contentType(type)
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(name).build().toString())
                    .body(new FileSystemResource(resolved));
        } catch (Exception e) { return ResponseEntity.badRequest().build(); }
    }

    private ApiResponse<String> upload(MultipartFile file, String... dirs) {
        if (file == null || file.isEmpty()) return ApiResponse.fail("文件为空");
        try {
            Path dir = root;
            for (String d : dirs) dir = dir.resolve(d).normalize();
            if (!dir.startsWith(root)) return ApiResponse.fail(400, "非法目录");
            Files.createDirectories(dir);
            String original = file.getOriginalFilename() == null ? "attachment" : Paths.get(file.getOriginalFilename()).getFileName().toString();
            Path target = dir.resolve(System.currentTimeMillis() + "_" + original).normalize();
            if (!target.startsWith(root)) return ApiResponse.fail(400, "非法文件名");
            file.transferTo(target.toFile());
            return ApiResponse.ok("uploads/" + root.relativize(target).toString().replace('\\', '/'));
        } catch (Exception e) { return ApiResponse.fail(500, "文件保存失败"); }
    }

    private Path resolve(String value) {
        String p = value == null ? "" : value.replace('\\', '/');
        int marker = p.indexOf("uploads/");
        if (marker >= 0) p = p.substring(marker + 8);
        Path resolved = root.resolve(Paths.get(p).normalize()).normalize();
        if (!resolved.startsWith(root)) throw new IllegalArgumentException("path outside uploads");
        return resolved;
    }
}
