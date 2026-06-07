package org.example.controller;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.example.entity.Course;
import org.example.entity.TeachingResource;
import org.example.entity.User;
import org.example.mapper.CourseEnrollmentMapper;
import org.example.mapper.TeachingResourceMapper;
import org.example.service.AiService;
import org.example.service.CourseService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequestMapping
public class TeachingResourceController {

    private static final String RESOURCE_DIR = "uploads" + File.separator + "resources" + File.separator;

    private final CourseService courseService;
    private final TeachingResourceMapper resourceMapper;
    private final CourseEnrollmentMapper enrollmentMapper;
    private final AiService aiService;

    public TeachingResourceController(CourseService courseService,
                                      TeachingResourceMapper resourceMapper,
                                      CourseEnrollmentMapper enrollmentMapper,
                                      AiService aiService) {
        this.courseService = courseService;
        this.resourceMapper = resourceMapper;
        this.enrollmentMapper = enrollmentMapper;
        this.aiService = aiService;
    }

    @GetMapping("/teacher/resource/manage/{courseId}")
    public String manageResources(@PathVariable Long courseId, HttpSession session, Model model) {
        User user = requireRole(session, "teacher");
        if (user == null) return "redirect:/login";
        Course course = ownedCourse(user, courseId);
        if (course == null) return "redirect:/teacher/course/manage";
        model.addAttribute("user", user);
        model.addAttribute("course", UserController.toCourseView(course));
        model.addAttribute("resources", resourceMapper.findByCourseId(courseId));
        return "teacher/resource_manage";
    }

    @PostMapping("/teacher/resource/upload")
    public String uploadResource(@RequestParam Long courseId,
                                 @RequestParam String title,
                                 @RequestParam MultipartFile file,
                                 HttpSession session,
                                 Model model) {
        User user = requireRole(session, "teacher");
        if (user == null) return "redirect:/login";
        Course course = ownedCourse(user, courseId);
        if (course == null) return "redirect:/teacher/course/manage";
        String type = detectType(file);
        if (type == null) {
            model.addAttribute("user", user);
            model.addAttribute("course", UserController.toCourseView(course));
            model.addAttribute("resources", resourceMapper.findByCourseId(courseId));
            model.addAttribute("error", "只支持上传 PDF、MP4、WebM、MOV 视频文件。");
            return "teacher/resource_manage";
        }
        String path = saveFile(courseId, file);
        if (path == null) {
            model.addAttribute("user", user);
            model.addAttribute("course", UserController.toCourseView(course));
            model.addAttribute("resources", resourceMapper.findByCourseId(courseId));
            model.addAttribute("error", "文件保存失败，请检查文件是否为空。");
            return "teacher/resource_manage";
        }
        TeachingResource resource = new TeachingResource();
        resource.setCourseId(courseId);
        resource.setTitle(title == null || title.trim().isEmpty() ? originalName(file) : title.trim());
        resource.setType(type);
        resource.setFilePath(path);
        resourceMapper.insert(resource);
        return "redirect:/teacher/resource/manage/" + courseId;
    }

    @GetMapping("/teacher/resource/upload")
    public String uploadEntry(@RequestParam(required = false) Long courseId) {
        return courseId == null ? "redirect:/teacher/course/manage" : "redirect:/teacher/resource/manage/" + courseId;
    }

    @PostMapping("/teacher/resource/delete")
    public String deleteResource(@RequestParam Long resourceId, HttpSession session) {
        User user = requireRole(session, "teacher");
        if (user == null) return "redirect:/login";
        TeachingResource resource = resourceMapper.findById(resourceId);
        if (resource == null) return "redirect:/teacher/course/manage";
        Course course = ownedCourse(user, resource.getCourseId());
        if (course == null) return "redirect:/teacher/course/manage";
        resourceMapper.deleteById(resourceId);
        return "redirect:/teacher/resource/manage/" + resource.getCourseId();
    }

    @GetMapping("/student/resource/download/{resourceId}")
    public ResponseEntity<Resource> downloadPdf(@PathVariable Long resourceId, HttpSession session) throws Exception {
        User user = requireRole(session, "student");
        TeachingResource resource = accessibleStudentResource(user, resourceId);
        if (resource == null || !resource.isPdf()) {
            return ResponseEntity.status(403).build();
        }
        Resource file = fileResource(resource);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
                .body(file);
    }

    @GetMapping("/student/resource/video/{resourceId}")
    public String videoPage(@PathVariable Long resourceId, HttpSession session, Model model) {
        User user = requireRole(session, "student");
        TeachingResource resource = accessibleStudentResource(user, resourceId);
        if (resource == null || !resource.isVideo()) return "redirect:/student/course/my";
        model.addAttribute("user", user);
        model.addAttribute("resource", resource);
        model.addAttribute("course", UserController.toCourseView(courseService.findById(resource.getCourseId())));
        return "student/resource_video";
    }

    @GetMapping("/student/resource/stream/{resourceId}")
    public ResponseEntity<Resource> streamVideo(@PathVariable Long resourceId, HttpSession session) throws Exception {
        User user = requireRole(session, "student");
        TeachingResource resource = accessibleStudentResource(user, resourceId);
        if (resource == null || !resource.isVideo()) {
            return ResponseEntity.status(403).build();
        }
        Resource file = fileResource(resource);
        return ResponseEntity.ok()
                .contentType(videoType(file.getFilename()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFilename() + "\"")
                .body(file);
    }

    @GetMapping("/student/resource/notes/{resourceId}")
    public String pdfNotes(@PathVariable Long resourceId, HttpSession session, Model model) {
        User user = requireRole(session, "student");
        TeachingResource resource = accessibleStudentResource(user, resourceId);
        if (resource == null || !resource.isPdf()) return "redirect:/student/course/my";
        model.addAttribute("user", user);
        model.addAttribute("resource", resource);
        model.addAttribute("course", UserController.toCourseView(courseService.findById(resource.getCourseId())));
        model.addAttribute("notes", generatePdfNotes(resource));
        return "student/resource_notes";
    }

    private User requireRole(HttpSession session, String role) {
        User user = UserController.requireUser(session);
        return user != null && role.equals(user.getRole()) ? user : null;
    }

    private Course ownedCourse(User user, Long courseId) {
        Course course = courseService.findById(courseId);
        return course != null && user.getId().equals(course.getTeacherId()) ? course : null;
    }

    private TeachingResource accessibleStudentResource(User user, Long resourceId) {
        if (user == null) return null;
        TeachingResource resource = resourceMapper.findById(resourceId);
        if (resource == null) return null;
        return enrollmentMapper.findByStudentAndCourse(user.getId(), resource.getCourseId()) == null ? null : resource;
    }

    private String saveFile(Long courseId, MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        try {
            Path root = Paths.get(RESOURCE_DIR, String.valueOf(courseId)).toAbsolutePath().normalize();
            Files.createDirectories(root);
            Path target = root.resolve(System.currentTimeMillis() + "_" + originalName(file)).normalize();
            if (!target.startsWith(root)) return null;
            Files.copy(file.getInputStream(), target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String detectType(MultipartFile file) {
        String filename = originalName(file).toLowerCase();
        String contentType = file == null ? "" : String.valueOf(file.getContentType()).toLowerCase();
        if (filename.endsWith(".pdf") || contentType.contains("pdf")) return "pdf";
        if (filename.endsWith(".mp4") || filename.endsWith(".webm") || filename.endsWith(".mov") || contentType.startsWith("video/")) {
            return "video";
        }
        return null;
    }

    private String originalName(MultipartFile file) {
        String filename = file == null ? "" : file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) return "resource";
        return Paths.get(filename).getFileName().toString();
    }

    private Resource fileResource(TeachingResource resource) throws Exception {
        Path path = Paths.get(resource.getFilePath()).toAbsolutePath().normalize();
        Resource file = new UrlResource(path.toUri());
        if (file.exists() && file.isReadable()) return file;
        throw new IllegalArgumentException("资源文件不存在或不可读");
    }

    private MediaType videoType(String filename) {
        String lower = filename == null ? "" : filename.toLowerCase();
        if (lower.endsWith(".webm")) return MediaType.parseMediaType("video/webm");
        if (lower.endsWith(".mov")) return MediaType.parseMediaType("video/quicktime");
        return MediaType.parseMediaType("video/mp4");
    }

    private String generatePdfNotes(TeachingResource resource) {
        try (PDDocument document = PDDocument.load(Paths.get(resource.getFilePath()).toFile())) {
            String text = new PDFTextStripper().getText(document);
            return aiService.summarizePdfText(resource.getCourseName(), resource.getTitle(), text);
        } catch (Exception e) {
            return "PDF 读取或 AI 笔记生成失败：" + e.getMessage();
        }
    }
}
