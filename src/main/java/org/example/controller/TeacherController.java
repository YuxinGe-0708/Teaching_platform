package org.example.controller;

import org.example.entity.Course;
import org.example.entity.CourseClass;
import org.example.entity.Submission;
import org.example.entity.Task;
import org.example.entity.User;
import org.example.mapper.CourseClassMapper;
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

import javax.servlet.http.HttpSession;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/teacher")
public class TeacherController {

    private final CourseService courseService;
    private final TaskService taskService;
    private final SubmissionMapper submissionMapper;
    private final CourseClassMapper courseClassMapper;

    public TeacherController(CourseService courseService,
                             TaskService taskService,
                             SubmissionMapper submissionMapper,
                             CourseClassMapper courseClassMapper) {
        this.courseService = courseService;
        this.taskService = taskService;
        this.submissionMapper = submissionMapper;
        this.courseClassMapper = courseClassMapper;
    }

    @GetMapping("/course/manage")
    public String manageCourses(HttpSession session, Model model,
                                @RequestParam(required = false) Long courseId) {
        User user = requireTeacher(session);
        if (user == null) return "redirect:/login";
        model.addAttribute("user", user);
        model.addAttribute("courses", toCourseViewsWithClasses(courseService.getTeacherCourses(user.getId())));
        model.addAttribute("highlightCourseId", courseId);
        return "teacher/course_manage";
    }

    @GetMapping("/course/create")
    public String showCreateCourse(HttpSession session, Model model) {
        User user = requireTeacher(session);
        if (user == null) return "redirect:/login";
        model.addAttribute("user", user);
        return "teacher/course_create";
    }

    @PostMapping("/course/create")
    public String createCourse(@RequestParam String courseName,
                               @RequestParam String courseCode,
                               @RequestParam Double credit,
                               @RequestParam(required = false) String description,
                               HttpSession session) {
        User user = requireTeacher(session);
        if (user == null) return "redirect:/login";
        Course course = new Course();
        course.setName(courseName);
        course.setCode(courseCode);
        course.setCredits(credit == null ? 0 : credit.intValue());
        course.setTeacherId(user.getId());
        course.setDescription(description);
        courseService.createCourse(course);
        return "redirect:/teacher/course/manage";
    }

    @GetMapping("/course/edit/{id}")
    public String editCourse(@PathVariable Long id, Model model, HttpSession session) {
        User user = requireTeacher(session);
        if (user == null) return "redirect:/login";
        Course course = ownedCourse(user, id);
        if (course == null) return "redirect:/teacher/course/manage";
        model.addAttribute("user", user);
        model.addAttribute("course", UserController.toCourseView(course));
        return "teacher/course_edit";
    }

    @PostMapping("/course/update")
    public String updateCourse(@RequestParam Long courseId,
                               @RequestParam String courseName,
                               @RequestParam String courseCode,
                               @RequestParam Double credit,
                               @RequestParam String description,
                               HttpSession session) {
        User user = requireTeacher(session);
        if (user == null) return "redirect:/login";
        Course course = ownedCourse(user, courseId);
        if (course != null) {
            course.setName(courseName);
            course.setCode(courseCode);
            course.setCredits(credit == null ? 0 : credit.intValue());
            course.setDescription(description);
            courseService.updateCourse(course);
        }
        return "redirect:/teacher/course/manage";
    }

    @GetMapping("/course/delete/{id}")
    public String deleteCourse(@PathVariable Long id, HttpSession session) {
        User user = requireTeacher(session);
        if (user == null) return "redirect:/login";
        Course course = ownedCourse(user, id);
        if (course != null) courseService.deleteCourse(id);
        return "redirect:/teacher/course/manage";
    }

    @GetMapping("/task/manage")
    public String manageTasks(HttpSession session, Model model) {
        User user = requireTeacher(session);
        if (user == null) return "redirect:/login";
        List<Course> courses = courseService.getTeacherCourses(user.getId());
        List<Map<String, Object>> tasks = courses.stream()
                .flatMap(course -> taskService.getCourseTasks(course.getId()).stream())
                .map(UserController::toTaskView)
                .collect(Collectors.toList());
        model.addAttribute("user", user);
        model.addAttribute("courses", UserController.toCourseViews(courses));
        model.addAttribute("tasks", tasks);
        return "teacher/task_manage";
    }

    @GetMapping("/task/create")
    public String showCreateTask(@RequestParam(required = false) Long courseId, Model model, HttpSession session) {
        User user = requireTeacher(session);
        if (user == null) return "redirect:/login";
        model.addAttribute("user", user);
        model.addAttribute("courses", UserController.toCourseViews(courseService.getTeacherCourses(user.getId())));
        model.addAttribute("selectedCourseId", courseId);
        return "teacher/task_create";
    }

    @PostMapping("/task/create")
    public String createTask(@RequestParam Long courseId,
                             @RequestParam String title,
                             @RequestParam String taskType,
                             @RequestParam(required = false) String description,
                             @RequestParam(required = false) String content,
                             @RequestParam(required = false) String allowedFileTypes,
                             @RequestParam(required = false) String endTime,
                             @RequestParam(required = false) String examAnswer,
                             @RequestParam(required = false) String sampleInput,
                             @RequestParam(required = false) String expectedOutput,
                             @RequestParam(required = false) Double fullScore,
                             HttpSession session) {
        User user = requireTeacher(session);
        if (user == null) return "redirect:/login";
        Course course = ownedCourse(user, courseId);
        if (course == null) return "redirect:/teacher/task/manage";
        Task task = new Task();
        task.setCourseId(courseId);
        task.setTitle(title);
        task.setType(UserController.dbTaskType(taskType));
        task.setMaxScore(fullScore == null ? 100 : fullScore.intValue());
        task.setDescription(TaskMetadataUtils.buildDescription(UserController.firstNonBlank(content, description), examAnswer, sampleInput, expectedOutput));
        task.setEndTime(parseTimestamp(endTime));
        taskService.createTask(task);
        return "redirect:/teacher/task/manage";
    }

    @GetMapping("/task/detail/{taskId}")
    public String taskDetail(@PathVariable Long taskId, Model model, HttpSession session) {
        User user = requireTeacher(session);
        if (user == null) return "redirect:/login";
        Task task = taskService.findById(taskId);
        if (!ownsTask(user, task)) return "redirect:/teacher/task/manage";
        model.addAttribute("user", user);
        model.addAttribute("task", UserController.toTaskView(task));
        model.addAttribute("taskContentHtml", MarkdownUtils.toHtml(TaskMetadataUtils.visibleMarkdown(task.getDescription())));
        model.addAttribute("submissions", submissionMapper.findByTaskId(taskId));
        return "teacher/task_detail";
    }

    @GetMapping("/task/grade/{submissionId}")
    public String showGradeForm(@PathVariable Long submissionId, Model model, HttpSession session) {
        User user = requireTeacher(session);
        if (user == null) return "redirect:/login";
        Submission submission = submissionMapper.findById(submissionId);
        if (submission == null || !ownsTask(user, taskService.findById(submission.getTaskId()))) {
            return "redirect:/teacher/task/manage";
        }
        model.addAttribute("user", user);
        model.addAttribute("submission", submission);
        return "teacher/task_grade";
    }

    @PostMapping("/task/grade")
    public String submitGrade(@RequestParam Long submissionId,
                              @RequestParam Double score,
                              @RequestParam(required = false) String comment,
                              HttpSession session) {
        User user = requireTeacher(session);
        if (user == null) return "redirect:/login";
        Submission submission = submissionMapper.findById(submissionId);
        if (submission != null && ownsTask(user, taskService.findById(submission.getTaskId()))) {
            submission.setScore(score);
            submission.setFeedback(comment);
            submission.setStatus("graded");
            submission.setJudgeResult(submission.getJudgeResult() == null ? "" : submission.getJudgeResult());
            submissionMapper.grade(submission);
        }
        return "redirect:/teacher/task/manage";
    }

    @GetMapping("/task/delete")
    public String deleteTask(@RequestParam Long taskId, HttpSession session) {
        User user = requireTeacher(session);
        if (user == null) return "redirect:/login";
        Task task = taskService.findById(taskId);
        if (ownsTask(user, task)) taskService.deleteTask(taskId);
        return "redirect:/teacher/task/manage";
    }

    @GetMapping("/task/download")
    public ResponseEntity<Resource> downloadSubmission(@RequestParam String filePath) throws Exception {
        Path path = Paths.get(filePath);
        Resource resource = new UrlResource(path.toUri());
        if (resource.exists() || resource.isReadable()) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        }
        throw new RuntimeException("Could not read the file");
    }

    @GetMapping("/course/class/{courseId}")
    public String manageClasses(@PathVariable Long courseId, HttpSession session, Model model) {
        User user = requireTeacher(session);
        if (user == null) return "redirect:/login";
        Course course = ownedCourse(user, courseId);
        if (course == null) return "redirect:/teacher/course/manage";
        List<CourseClass> classes = ensureClasses(course);
        model.addAttribute("user", user);
        model.addAttribute("course", UserController.toCourseView(course));
        model.addAttribute("classes", classes);
        return "teacher/class_manage";
    }

    @PostMapping("/course/class/create")
    public String createClass(@RequestParam Long courseId,
                              @RequestParam String className,
                              @RequestParam(required = false, defaultValue = "100") Integer maxCount,
                              HttpSession session) {
        User user = requireTeacher(session);
        if (user == null) return "redirect:/login";
        Course course = ownedCourse(user, courseId);
        if (course != null) {
            CourseClass courseClass = new CourseClass();
            courseClass.setCourseId(courseId);
            courseClass.setName(className);
            courseClass.setInviteCode(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            courseClass.setMaxCount(maxCount == null ? 100 : maxCount);
            courseClass.setCurrentCount(0);
            courseClassMapper.insert(courseClass);
        }
        return "redirect:/teacher/course/class/" + courseId;
    }

    @GetMapping("/course/class/delete")
    public String deleteClass(@RequestParam Long courseId, @RequestParam Long classId, HttpSession session) {
        User user = requireTeacher(session);
        if (user == null) return "redirect:/login";
        Course course = ownedCourse(user, courseId);
        if (course != null) {
            courseClassMapper.deleteByCourse(courseId, classId);
        }
        return "redirect:/teacher/course/class/" + courseId;
    }

    @GetMapping("/score/statistics")
    public String scoreStatistics(HttpSession session, Model model) {
        User user = requireTeacher(session);
        if (user == null) return "redirect:/login";
        List<Course> courses = courseService.getTeacherCourses(user.getId());
        model.addAttribute("user", user);
        model.addAttribute("courses", UserController.toCourseViews(courses));
        model.addAttribute("stats", courses.stream().map(this::courseStats).collect(Collectors.toList()));
        return "teacher/score_statistics";
    }

    private User requireTeacher(HttpSession session) {
        User user = UserController.requireUser(session);
        return user != null && "teacher".equals(user.getRole()) ? user : null;
    }

    private Course ownedCourse(User user, Long courseId) {
        Course course = courseService.findById(courseId);
        return course != null && user.getId().equals(course.getTeacherId()) ? course : null;
    }

    private boolean ownsTask(User user, Task task) {
        if (task == null) return false;
        Course course = courseService.findById(task.getCourseId());
        return course != null && user.getId().equals(course.getTeacherId());
    }

    private Timestamp parseTimestamp(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        String text = raw.trim().replace('T', ' ');
        if (text.length() == 16) text += ":00";
        return Timestamp.valueOf(LocalDateTime.parse(text.replace(' ', 'T')));
    }

    private List<Map<String, Object>> toCourseViewsWithClasses(List<Course> courses) {
        return courses.stream().map(course -> {
            Map<String, Object> view = UserController.toCourseView(course);
            List<Map<String, Object>> classes = ensureClasses(course).stream()
                    .map(this::toClassView)
                    .collect(Collectors.toList());
            view.put("classes", classes);
            return view;
        }).collect(Collectors.toList());
    }

    private List<CourseClass> ensureClasses(Course course) {
        List<CourseClass> classes = courseClassMapper.findByCourseId(course.getId());
        if (!classes.isEmpty()) return classes;
        CourseClass defaultClass = new CourseClass();
        defaultClass.setCourseId(course.getId());
        defaultClass.setName("默认班级");
        defaultClass.setInviteCode(course.getInviteCode() == null || course.getInviteCode().isEmpty()
                ? UUID.randomUUID().toString().substring(0, 8).toUpperCase()
                : course.getInviteCode());
        defaultClass.setMaxCount(100);
        defaultClass.setCurrentCount(course.getStudentCount() == null ? 0 : course.getStudentCount());
        courseClassMapper.insert(defaultClass);
        return courseClassMapper.findByCourseId(course.getId());
    }

    private Map<String, Object> toClassView(CourseClass courseClass) {
        Map<String, Object> view = new HashMap<>();
        view.put("classId", courseClass.getId());
        view.put("courseId", courseClass.getCourseId());
        view.put("className", courseClass.getName());
        view.put("inviteCode", courseClass.getInviteCode());
        view.put("currentCount", courseClass.getCurrentCount() == null ? 0 : courseClass.getCurrentCount());
        view.put("maxCount", courseClass.getMaxCount() == null ? 100 : courseClass.getMaxCount());
        return view;
    }

    private Map<String, Object> courseStats(Course course) {
        List<Task> tasks = taskService.getCourseTasks(course.getId());
        List<Submission> submissions = submissionMapper.findByCourseId(course.getId());
        long gradedCount = submissions.stream().filter(s -> s.getScore() != null).count();
        double averageScore = submissions.stream()
                .filter(s -> s.getScore() != null)
                .mapToDouble(Submission::getScore)
                .average()
                .orElse(0);

        Map<String, Object> stats = new HashMap<>();
        stats.put("courseId", course.getId());
        stats.put("courseName", course.getName());
        stats.put("taskCount", tasks.size());
        stats.put("submissionCount", submissions.size());
        stats.put("gradedCount", gradedCount);
        stats.put("averageScore", Math.round(averageScore * 10.0) / 10.0);
        return stats;
    }
}
