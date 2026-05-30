package org.example.controller;

import org.example.entity.Course;
import org.example.entity.OperationLog;
import org.example.entity.Submission;
import org.example.entity.Task;
import org.example.entity.User;
import org.example.mapper.OperationLogMapper;
import org.example.mapper.SubmissionMapper;
import org.example.service.CourseService;
import org.example.service.TaskService;
import org.example.util.MarkdownUtils;
import org.example.util.TaskMetadataUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/student")
public class StudentController {

    private static final String UPLOAD_DIR = "uploads" + File.separator;

    private final CourseService courseService;
    private final TaskService taskService;
    private final SubmissionMapper submissionMapper;
    private final OperationLogMapper operationLogMapper;

    public StudentController(CourseService courseService,
                             TaskService taskService,
                             SubmissionMapper submissionMapper,
                             OperationLogMapper operationLogMapper) {
        this.courseService = courseService;
        this.taskService = taskService;
        this.submissionMapper = submissionMapper;
        this.operationLogMapper = operationLogMapper;
    }

    @GetMapping("/course/selection")
    public String showCourseSelection(HttpSession session, Model model, @RequestParam(required = false) String search) {
        User user = requireStudent(session);
        if (user == null) return "redirect:/login";
        List<Course> courses = courseService.getAllActiveCourses();
        if (search != null && !search.trim().isEmpty()) {
            String lower = search.trim().toLowerCase();
            courses = courses.stream()
                    .filter(c -> contains(c.getName(), lower) || contains(c.getCode(), lower))
                    .collect(Collectors.toList());
        }
        Set<Long> myCourseIds = courseService.getStudentCourses(user.getId()).stream()
                .map(Course::getId)
                .collect(Collectors.toSet());
        model.addAttribute("user", user);
        model.addAttribute("courses", UserController.toCourseViews(courses));
        model.addAttribute("myCourseIds", myCourseIds);
        model.addAttribute("searchQuery", search);
        return "student/course_selection";
    }

    @PostMapping("/course/select")
    public String selectCourse(@RequestParam Long courseId, HttpSession session) {
        User user = requireStudent(session);
        if (user == null) return "redirect:/login";
        Course course = courseService.findById(courseId);
        if (course != null && courseService.enroll(user.getId(), courseId)) {
            log(user, "选课", course.getName());
        }
        return "redirect:/student/course/selection";
    }

    @PostMapping("/course/drop")
    public String dropCourse(@RequestParam Long courseId, HttpSession session) {
        User user = requireStudent(session);
        if (user == null) return "redirect:/login";
        Course course = courseService.findById(courseId);
        if (course != null && courseService.unenroll(user.getId(), courseId)) {
            log(user, "退课", course.getName());
        }
        return "redirect:/student/course/my";
    }

    @GetMapping("/course/my")
    public String myCourses(HttpSession session, Model model) {
        User user = requireStudent(session);
        if (user == null) return "redirect:/login";
        model.addAttribute("user", user);
        model.addAttribute("courses", UserController.toCourseViews(courseService.getStudentCourses(user.getId())));
        return "student/my_courses";
    }

    @GetMapping("/logs")
    public String viewLogs(HttpSession session, Model model) {
        User user = requireStudent(session);
        if (user == null) return "redirect:/login";
        List<String> logs = operationLogMapper.findByUserId(user.getId()).stream()
                .map(log -> log.getCreatedAt() + " - 用户 [" + log.getUsername() + "] " + log.getAction() + "：" + log.getDetail())
                .collect(Collectors.toList());
        model.addAttribute("user", user);
        model.addAttribute("logs", logs);
        return "student/logs";
    }

    @GetMapping("/course/detail/{courseId}")
    public String courseDetail(@PathVariable Long courseId,
                               @RequestParam(required = false) String tab,
                               HttpSession session,
                               Model model) {
        User user = requireStudent(session);
        if (user == null) return "redirect:/login";
        Course course = courseService.findById(courseId);
        if (course == null) return "redirect:/student/course/my";
        String activeTab = (tab == null || tab.trim().isEmpty()) ? "home" : tab.trim();
        List<java.util.Map<String, Object>> tasks = taskService.getCourseTasks(courseId).stream()
                .filter(task -> filterTask(activeTab, task))
                .map(UserController::toTaskView)
                .collect(Collectors.toList());
        model.addAttribute("user", user);
        model.addAttribute("course", UserController.toCourseView(course));
        model.addAttribute("tasks", tasks);
        model.addAttribute("tab", activeTab);
        return "student/course_detail";
    }

    @GetMapping("/task/detail")
    public String taskDetail(@RequestParam Long taskId, HttpSession session, Model model) {
        User user = requireStudent(session);
        if (user == null) return "redirect:/login";
        Task task = taskService.findById(taskId);
        if (task == null) return "redirect:/student/course/my";
        Course course = courseService.findById(task.getCourseId());
        Submission submission = taskService.getSubmission(user.getId(), taskId);
        String visibleMarkdown = TaskMetadataUtils.visibleMarkdown(task.getDescription());
        model.addAttribute("user", user);
        model.addAttribute("task", UserController.toTaskView(task));
        model.addAttribute("course", course == null ? null : UserController.toCourseView(course));
        model.addAttribute("submission", submission);
        model.addAttribute("taskContentHtml", MarkdownUtils.toHtml(visibleMarkdown));
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
                             @RequestParam(required = false) String content,
                             HttpSession session,
                             Model model) {
        User user = requireStudent(session);
        if (user == null) return "redirect:/login";
        Task task = taskService.findById(taskId);
        if (task == null) return "redirect:/student/course/my";
        String filePath = saveFile(file);
        String finalContent = content == null ? "" : content;
        if (finalContent.trim().isEmpty() && filePath == null) {
            model.addAttribute("error", "提交内容或附件不能为空");
            return taskDetail(taskId, session, model);
        }
        Submission submission = taskService.getSubmission(user.getId(), taskId);
        if (submission == null) {
            submission = new Submission();
            submission.setTaskId(taskId);
            submission.setStudentId(user.getId());
            submission.setContent(finalContent);
            submission.setFilePath(filePath == null ? "" : filePath);
            submission.setStatus("submitted");
            submission.setJudgeResult("");
            submissionMapper.insert(submission);
        } else {
            submission.setContent(finalContent);
            submission.setFilePath(filePath == null ? submission.getFilePath() : filePath);
            submissionMapper.updateContent(submission);
        }
        if ("exam".equals(task.getType())) {
            autoGradeExam(task, submission, finalContent);
        }
        session.setAttribute("uploadSuccess", "提交成功");
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
        }
        throw new RuntimeException("Could not read the file");
    }

    private void autoGradeExam(Task task, Submission submission, String content) {
        String answer = TaskMetadataUtils.examAnswer(task.getDescription());
        if (answer == null || answer.trim().isEmpty()) return;
        boolean correct = answer.trim().equalsIgnoreCase(content == null ? "" : content.trim());
        submission.setScore(correct ? Double.valueOf(task.getMaxScore() == null ? 100 : task.getMaxScore()) : 0D);
        submission.setStatus("graded");
        submission.setJudgeResult(correct ? "AC" : "WA");
        submission.setFeedback(correct ? "系统自动判分：答案正确。" : "系统自动判分：答案与标准答案不一致。");
        submissionMapper.grade(submission);
    }

    private User requireStudent(HttpSession session) {
        User user = UserController.requireUser(session);
        return user != null && "student".equals(user.getRole()) ? user : null;
    }

    private boolean filterTask(String tab, Task task) {
        if ("homework".equals(tab)) return "homework".equals(task.getType());
        if ("exam".equals(tab)) return "exam".equals(task.getType());
        if ("lab".equals(tab)) return "programming".equals(task.getType());
        return true;
    }

    private void log(User user, String action, String detail) {
        OperationLog log = new OperationLog();
        log.setUserId(user.getId());
        log.setUsername(user.getUsername());
        log.setAction(action);
        log.setDetail(detail);
        operationLogMapper.insert(log);
    }

    private String saveFile(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        try {
            String filename = file.getOriginalFilename();
            if (filename == null || filename.trim().isEmpty()) return null;
            filename = Paths.get(filename).getFileName().toString();
            Path uploadRoot = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            Files.createDirectories(uploadRoot);
            Path target = uploadRoot.resolve(System.currentTimeMillis() + "_" + filename);
            Files.copy(file.getInputStream(), target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean contains(String value, String lower) {
        return value != null && value.toLowerCase().contains(lower);
    }
}
