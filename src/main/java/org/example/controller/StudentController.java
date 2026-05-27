package org.example.controller;

import org.example.model.Course;
import org.example.model.Task;
import org.example.model.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.example.model.Submission;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.File;
import java.io.IOException;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.example.util.MarkdownUtils;

@Controller
@RequestMapping("/student")
public class StudentController {
    
    // In-memory data store for course selections
    public static final Map<String, List<Long>> studentCourses = new ConcurrentHashMap<>();
    public static final List<String> activityLogs = new ArrayList<>();

    private void addLog(String username, String action, String courseName) {
        String logEntry = new Date().toString() + " - 用户 [" + username + "] " + action + "了课程: " + courseName;
        activityLogs.add(logEntry);
    }

    @GetMapping("/course/selection")
    public String showCourseSelection(HttpSession session, Model model, @RequestParam(required = false) String search) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null || !"student".equals(user.getRole())) return "redirect:/login";

        List<Course> availableCourses = TeacherController.courseDatabase;
        if (search != null && !search.trim().isEmpty()) {
            String lowerSearch = search.toLowerCase();
            availableCourses = availableCourses.stream()
                .filter(c -> c.getCourseCode().toLowerCase().contains(lowerSearch) || c.getCourseName().toLowerCase().contains(lowerSearch))
                .collect(Collectors.toList());
        }

        List<Long> myCourseIds = studentCourses.getOrDefault(user.getUsername(), new ArrayList<>());
        
        model.addAttribute("courses", availableCourses);
        model.addAttribute("myCourseIds", myCourseIds);
        model.addAttribute("searchQuery", search);
        return "student/course_selection";
    }

    @PostMapping("/course/select")
    public String selectCourse(@RequestParam Long courseId, HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null || !"student".equals(user.getRole())) return "redirect:/login";

        Course course = TeacherController.courseDatabase.stream()
            .filter(c -> c.getCourseId().equals(courseId))
            .findFirst().orElse(null);

        if (course != null) {
            List<Long> enrolled = studentCourses.computeIfAbsent(user.getUsername(), k -> new ArrayList<>());
            if (!enrolled.contains(courseId)) {
                enrolled.add(courseId);
                addLog(user.getUsername(), "选修", course.getCourseName());
            }
        }
        return "redirect:/student/course/selection";
    }

    @PostMapping("/course/drop")
    public String dropCourse(@RequestParam Long courseId, HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null || !"student".equals(user.getRole())) return "redirect:/login";

        Course course = TeacherController.courseDatabase.stream()
            .filter(c -> c.getCourseId().equals(courseId))
            .findFirst().orElse(null);

        if (course != null) {
            List<Long> enrolled = studentCourses.get(user.getUsername());
            if (enrolled != null && enrolled.contains(courseId)) {
                enrolled.remove(courseId);
                addLog(user.getUsername(), "退选", course.getCourseName());
            }
        }
        return "redirect:/student/course/my";
    }

    @GetMapping("/course/my")
    public String myCourses(HttpSession session, Model model) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null || !"student".equals(user.getRole())) return "redirect:/login";

        List<Long> myCourseIds = studentCourses.getOrDefault(user.getUsername(), new ArrayList<>());
        List<Course> myCourses = TeacherController.courseDatabase.stream()
            .filter(c -> myCourseIds.contains(c.getCourseId()))
            .collect(Collectors.toList());

        model.addAttribute("courses", myCourses);
        return "student/my_courses";
    }

    @GetMapping("/logs")
    public String viewLogs(HttpSession session, Model model) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null || !"student".equals(user.getRole())) return "redirect:/login";

        // Filter logs for this student
        List<String> myLogs = activityLogs.stream()
            .filter(log -> log.contains("用户 [" + user.getUsername() + "]"))
            .collect(Collectors.toList());
            
        model.addAttribute("logs", myLogs);
        return "student/logs";
    }

    @GetMapping("/course/detail/{courseId}")
    public String courseDetail(@PathVariable Long courseId,
                               @RequestParam(required = false) String tab,
                               HttpSession session,
                               Model model) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null || !"student".equals(user.getRole())) return "redirect:/login";

        Course course = TeacherController.courseDatabase.stream()
            .filter(c -> c.getCourseId().equals(courseId))
            .findFirst().orElse(null);

        if (course == null) return "redirect:/student/course/my";

        String activeTab = (tab == null || tab.trim().isEmpty()) ? "home" : tab.trim();

        List<Task> allTasks = TeacherController.taskDatabase.stream()
            .filter(t -> t.getCourseId().equals(courseId))
            .collect(Collectors.toList());

        List<Task> filteredTasks = allTasks;
        if ("homework".equals(activeTab)) {
            filteredTasks = allTasks.stream()
                .filter(t -> "作业".equals(t.getTaskType()))
                .collect(Collectors.toList());
        } else if ("exam".equals(activeTab)) {
            filteredTasks = allTasks.stream()
                .filter(t -> "考试".equals(t.getTaskType()))
                .collect(Collectors.toList());
        } else if ("lab".equals(activeTab)) {
            filteredTasks = allTasks.stream()
                .filter(t -> "编程实训".equals(t.getTaskType()))
                .collect(Collectors.toList());
        }

        model.addAttribute("user", user);
        model.addAttribute("course", course);
        model.addAttribute("tasks", filteredTasks);
        model.addAttribute("tab", activeTab);
        return "student/course_detail";
    }

    private static final String UPLOAD_DIR = "uploads" + File.separator;

    @GetMapping("/task/detail")
    public String taskDetail(@RequestParam Long taskId, HttpSession session, Model model) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null || !"student".equals(user.getRole())) return "redirect:/login";

        Task task = TeacherController.taskDatabase.stream()
            .filter(t -> t.getTaskId().equals(taskId))
            .findFirst().orElse(null);

        if (task == null) return "redirect:/student/course/my";

        Submission submission = TeacherController.submissionDatabase.stream()
            .filter(s -> s.getTaskId().equals(taskId) && s.getStudentName().equals(user.getUsername()))
            .findFirst().orElse(null);

        Course course = TeacherController.courseDatabase.stream()
            .filter(c -> c.getCourseId().equals(task.getCourseId()))
            .findFirst().orElse(null);

        model.addAttribute("task", task);
        model.addAttribute("course", course);
        model.addAttribute("submission", submission);
        model.addAttribute("taskContentHtml", MarkdownUtils.toHtml(task.getContent()));
        Object uploadSuccess = session.getAttribute("uploadSuccess");
        if (uploadSuccess != null) {
            model.addAttribute("success", uploadSuccess.toString());
            session.removeAttribute("uploadSuccess");
        }
        return "student/task_detail";
    }

    @PostMapping("/task/submit")
    public String taskSubmit(@RequestParam Long taskId, 
                             @RequestParam(required = false) MultipartFile file, 
                             HttpSession session, 
                             Model model) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null || !"student".equals(user.getRole())) return "redirect:/login";

        Task task = TeacherController.taskDatabase.stream()
            .filter(t -> t.getTaskId().equals(taskId))
            .findFirst().orElse(null);

        if (task == null) return "redirect:/student/course/my";

        String filePath = null;
        if (file == null || file.isEmpty()) {
            model.addAttribute("error", "请选择要上传的文件");
            return taskDetail(taskId, session, model);
        }
        try {
            String allowed = task.getAllowedFileTypes();
            String filename = file.getOriginalFilename();
            if (filename == null || filename.trim().isEmpty()) {
                model.addAttribute("error", "文件名无效，请重新选择文件");
                return taskDetail(taskId, session, model);
            }
            filename = Paths.get(filename).getFileName().toString();
            if (allowed != null && !allowed.trim().isEmpty()) {
                boolean isValid = false;
                String[] types = allowed.split(",");
                for (String type : types) {
                    String ext = type.trim().toLowerCase();
                    if (!ext.startsWith(".")) {
                        ext = "." + ext;
                    }
                    if (filename.toLowerCase().endsWith(ext)) {
                        isValid = true;
                        break;
                    }
                }
                if (!isValid) {
                    model.addAttribute("error", "文件格式不正确，允许的格式: " + allowed);
                    return taskDetail(taskId, session, model);
                }
            }

            Path uploadRoot = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            Files.createDirectories(uploadRoot);

            Path target = uploadRoot.resolve(System.currentTimeMillis() + "_" + filename);
            Files.copy(file.getInputStream(), target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            filePath = target.toString();
        } catch (Exception e) {
            model.addAttribute("error", "上传失败，请稍后重试");
            return taskDetail(taskId, session, model);
        }

        Submission submission = TeacherController.submissionDatabase.stream()
            .filter(s -> s.getTaskId().equals(taskId) && s.getStudentName().equals(user.getUsername()))
            .findFirst().orElse(null);

        if (submission == null) {
            submission = new Submission();
            submission.setSubmissionId(System.currentTimeMillis());
            submission.setTaskId(taskId);
            submission.setStudentId(Long.valueOf(user.getUsername().hashCode()));
            submission.setStudentName(user.getUsername());
            TeacherController.submissionDatabase.add(submission);
        }

        if (filePath != null) {
            submission.setFileUrl(filePath);
        }
        submission.setSubmitTime(new Date());
        submission.setSubmitStatus("已提交");
        session.setAttribute("uploadSuccess", "作业上传成功");
        return "redirect:/student/task/detail?taskId=" + taskId;
    }

    @GetMapping("/task/download")
    public ResponseEntity<Resource> downloadTaskFile(@RequestParam String filePath) throws Exception {
        Path path = Paths.get(filePath);
        Resource resource = new UrlResource(path.toUri());

        if (resource.exists() || resource.isReadable()) {
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
        } else {
            throw new RuntimeException("Could not read the file!");
        }
    }
}
