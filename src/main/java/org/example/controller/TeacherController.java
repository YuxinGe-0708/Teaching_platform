package org.example.controller;

import org.example.entity.Course;
import org.example.entity.CourseClass;
import org.example.entity.CourseEnrollment;
import org.example.entity.ExamRecord;
import org.example.entity.Submission;
import org.example.entity.Task;
import org.example.entity.User;
import org.example.mapper.CourseClassMapper;
import org.example.mapper.CourseEnrollmentMapper;
import org.example.mapper.ExamRecordMapper;
import org.example.mapper.SubmissionMapper;
import org.example.mapper.UserMapper;
import org.example.service.CourseService;
import org.example.service.ScoreService;
import org.example.service.TaskService;
import org.example.bff.MicroserviceClient;
import org.springframework.beans.factory.annotation.Value;
import org.example.util.DownloadUtils;
import org.example.util.ExamContentUtils;
import org.example.util.MarkdownUtils;
import org.example.util.TaskMetadataUtils;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private final CourseEnrollmentMapper enrollmentMapper;
    private final UserMapper userMapper;
    private final ScoreService scoreService;
    private final ExamRecordMapper examRecordMapper;
    private final MicroserviceClient microservices;
    private final boolean bffEnabled;

    public TeacherController(CourseService courseService,
                             TaskService taskService,
                             SubmissionMapper submissionMapper,
                             CourseClassMapper courseClassMapper,
                             CourseEnrollmentMapper enrollmentMapper,
                             UserMapper userMapper,
                             ScoreService scoreService,
                             ExamRecordMapper examRecordMapper,
                             MicroserviceClient microservices,
                             @Value("${app.bff.enabled:false}") boolean bffEnabled) {
        this.courseService = courseService;
        this.taskService = taskService;
        this.submissionMapper = submissionMapper;
        this.courseClassMapper = courseClassMapper;
        this.enrollmentMapper = enrollmentMapper;
        this.userMapper = userMapper;
        this.scoreService = scoreService;
        this.examRecordMapper = examRecordMapper;
        this.microservices = microservices;
        this.bffEnabled = bffEnabled;
    }

    @GetMapping("/course/manage")
    public String manageCourses(HttpSession session, Model model,
                                @RequestParam(required = false) Long courseId,
                                @RequestParam(required = false) String keyword,
                                @RequestParam(required = false, defaultValue = "newest") String sort) {
        User user = requireTeacher(session);
        if (user == null) return "redirect:/login";
        List<Course> courses = (keyword == null || keyword.trim().isEmpty()) && "newest".equals(sort)
                ? courseService.getTeacherCourses(user.getId())
                : courseService.searchTeacherCourses(user.getId(), keyword, sort);
        model.addAttribute("user", user);
        model.addAttribute("courses", toCourseViewsWithClasses(courses));
        model.addAttribute("highlightCourseId", courseId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);
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
                               @RequestParam(required = false) String subjectCategory,
                               @RequestParam(required = false) Integer hours,
                               @RequestParam(required = false, defaultValue = "true") Boolean allowJoin,
                               @RequestParam(required = false, defaultValue = "active") String status,
                               @RequestParam(required = false) String description,
                               HttpSession session) {
        User user = requireTeacher(session);
        if (user == null) return "redirect:/login";
        Course course = new Course();
        course.setName(courseName);
        course.setCode(courseCode);
        course.setCredits(credit == null ? 0 : credit.intValue());
        course.setSubjectCategory(subjectCategory);
        course.setHours(hours == null ? 0 : hours);
        course.setAllowJoin(allowJoin == null || allowJoin);
        course.setStatus(normalizeCourseStatus(status));
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
                               @RequestParam(required = false) String subjectCategory,
                               @RequestParam(required = false) Integer hours,
                               @RequestParam(required = false, defaultValue = "true") Boolean allowJoin,
                               @RequestParam String description,
                               HttpSession session) {
        User user = requireTeacher(session);
        if (user == null) return "redirect:/login";
        Course course = ownedCourse(user, courseId);
        if (course != null) {
            course.setName(courseName);
            course.setCode(courseCode);
            course.setCredits(credit == null ? 0 : credit.intValue());
            course.setSubjectCategory(subjectCategory);
            course.setHours(hours == null ? 0 : hours);
            course.setAllowJoin(allowJoin == null || allowJoin);
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
        if (course != null) {
            if (course.getStudentCount() != null && course.getStudentCount() > 0) {
                courseService.updateStatus(id, "archived");
            } else {
                courseService.deleteCourse(id);
            }
        }
        return "redirect:/teacher/course/manage";
    }

    @GetMapping("/course/archive/{id}")
    public String archiveCourse(@PathVariable Long id, HttpSession session) {
        User user = requireTeacher(session);
        if (user == null) return "redirect:/login";
        Course course = ownedCourse(user, id);
        if (course != null) courseService.updateStatus(id, "archived");
        return "redirect:/teacher/course/manage";
    }

    @GetMapping("/task/manage")
    public String manageTasks(HttpSession session, Model model) {
        User user = requireTeacher(session);
        if (user == null) return "redirect:/login";
        List<Course> courses = courseService.getTeacherCourses(user.getId());
        List<Map<String, Object>> taskRows = courses.stream()
                .flatMap(course -> taskService.getCourseTasks(course.getId()).stream())
                .map(UserController::toTaskView)
                .collect(Collectors.toList());
        Map<Object, Map<String, Object>> uniqueTasks = new LinkedHashMap<>();
        for (Map<String, Object> taskRow : taskRows) {
            uniqueTasks.putIfAbsent(taskRow.get("taskId"), taskRow);
        }
        model.addAttribute("user", user);
        model.addAttribute("courses", UserController.toCourseViews(courses));
        model.addAttribute("tasks", new ArrayList<>(uniqueTasks.values()));
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
                             @RequestParam(required = false) String testCases,
                             @RequestParam(required = false) String allowedLanguage,
                             @RequestParam(required = false) String examQuestions,
                             @RequestParam(required = false) Integer timeLimitMs,
                             @RequestParam(required = false) Integer memoryLimitMb,
                             @RequestParam(required = false, defaultValue = "published") String status,
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
        task.setDescription(TaskMetadataUtils.buildDescription(
                UserController.firstNonBlank(content, description),
                examAnswer,
                testCases,
                allowedLanguage,
                examQuestions));
        task.setTimeLimitMs(timeLimitMs == null ? 15000 : timeLimitMs);
        task.setMemoryLimitMb(memoryLimitMb == null ? 128 : memoryLimitMb);
        task.setCodeTemplate(null);
        task.setEndTime(parseTimestamp(endTime));
        task.setStatus(normalizeTaskStatus(status));
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
        List<Submission> submissions = submissionMapper.findByTaskId(taskId);
        model.addAttribute("submissions", submissions);
        if ("exam".equals(task.getType())) {
            model.addAttribute("examRecords", examRecordMapper.findByTaskId(taskId));
            Map<Long, Integer> examAttachmentCounts = new HashMap<>();
            for (Submission submission : submissions) {
                if (submission.getId() != null) {
                    examAttachmentCounts.put(submission.getId(), ExamContentUtils.attachmentCount(submission.getContent()));
                }
            }
            model.addAttribute("examAttachmentCounts", examAttachmentCounts);
        }
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
        Task task = taskService.findById(submission.getTaskId());
        model.addAttribute("user", user);
        model.addAttribute("submission", submission);
        model.addAttribute("task", UserController.toTaskView(task));
        if (task != null && "exam".equals(task.getType())) {
            model.addAttribute("questionRows", ExamContentUtils.questionRows(
                    TaskMetadataUtils.examQuestionsJson(task.getDescription()),
                    submission.getContent(),
                    submission.getFeedback()));
            model.addAttribute("visibleComment", ExamContentUtils.visibleFeedback(submission.getFeedback()));
        }
        return "teacher/task_grade";
    }

    @PostMapping("/task/grade")
    public String submitGrade(@RequestParam Long submissionId,
                              @RequestParam(required = false) Double score,
                              @RequestParam(required = false) String comment,
                              @RequestParam(required = false) String questionScores,
                              @RequestParam(required = false) String questionComments,
                              HttpSession session) {
        User user = requireTeacher(session);
        if (user == null) return "redirect:/login";
        Submission submission = submissionMapper.findById(submissionId);
        if (submission != null && ownsTask(user, taskService.findById(submission.getTaskId()))) {
            Task task = taskService.findById(submission.getTaskId());
            Double finalScore = score;
            if (task != null && "exam".equals(task.getType()) && questionScores != null && !questionScores.trim().isEmpty()) {
                finalScore = sumScores(questionScores);
                submission.setFeedback(ExamContentUtils.buildFeedback(comment, questionScores, questionComments));
            } else {
                submission.setFeedback(comment);
            }
            submission.setScore(finalScore == null ? 0D : finalScore);
            submission.setStatus("graded");
            submission.setJudgeResult(submission.getJudgeResult() == null ? "" : submission.getJudgeResult());
            submissionMapper.grade(submission);
            if (task != null && "exam".equals(task.getType())) {
                ExamRecord record = examRecordMapper.findByStudentAndTask(submission.getStudentId(), submission.getTaskId());
                if (record != null) {
                    record.setScore(submission.getScore());
                    if (record.getStatus() == null || !record.isSubmitted()) {
                        record.setStatus("SUBMITTED");
                    }
                    if (record.getSubmitTime() == null) {
                        record.setSubmitTime(new Timestamp(System.currentTimeMillis()));
                    }
                    examRecordMapper.submit(record);
                }
            }
        }
        return submission == null ? "redirect:/teacher/task/manage" : "redirect:/teacher/task/detail/" + submission.getTaskId();
    }

    @GetMapping("/task/delete")
    public String deleteTask(@RequestParam Long taskId, HttpSession session) {
        User user = requireTeacher(session);
        if (user == null) return "redirect:/login";
        Task task = taskService.findById(taskId);
        if (ownsTask(user, task)) taskService.updateStatus(taskId, "retracted");
        return "redirect:/teacher/task/manage";
    }

    @PostMapping("/task/status")
    public String updateTaskStatus(@RequestParam Long taskId,
                                   @RequestParam String status,
                                   @RequestParam(required = false) String endTime,
                                   HttpSession session) {
        User user = requireTeacher(session);
        if (user == null) return "redirect:/login";
        Task task = taskService.findById(taskId);
        if (ownsTask(user, task)) {
            if (endTime != null && !endTime.trim().isEmpty()) {
                task.setEndTime(parseTimestamp(endTime));
                taskService.updateTask(task);
            }
            taskService.updateStatus(taskId, normalizeTaskStatus(status));
        }
        return "redirect:/teacher/task/manage";
    }

    @GetMapping("/task/download")
    public ResponseEntity<Resource> downloadSubmission(@RequestParam String filePath) throws Exception {
        if (!bffEnabled) return DownloadUtils.attachment(filePath);
        ResponseEntity<byte[]> remote = microservices.file(microservices.uri(microservices.assessment("/internal/files")).queryParam("path", filePath).toUriString());
        if (remote.getBody() == null || remote.getBody().length == 0) return ResponseEntity.status(remote.getStatusCode()).build();
        ResponseEntity.BodyBuilder b = ResponseEntity.status(remote.getStatusCode());
        if (remote.getHeaders().getContentType() != null) b.contentType(remote.getHeaders().getContentType());
        String cd = remote.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION); if (cd != null) b.header(HttpHeaders.CONTENT_DISPOSITION, cd);
        return b.body(new ByteArrayResource(remote.getBody()));
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
        model.addAttribute("members", classMemberViews(courseId));
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

    @PostMapping("/course/class/update")
    public String updateClass(@RequestParam Long courseId,
                              @RequestParam Long classId,
                              @RequestParam String className,
                              @RequestParam(required = false, defaultValue = "100") Integer maxCount,
                              HttpSession session) {
        User user = requireTeacher(session);
        if (user == null) return "redirect:/login";
        Course course = ownedCourse(user, courseId);
        if (course != null) {
            CourseClass courseClass = courseClassMapper.findById(classId);
            if (courseClass != null && courseId.equals(courseClass.getCourseId())) {
                courseClass.setName(className);
                courseClass.setMaxCount(maxCount == null ? 100 : maxCount);
                courseClassMapper.update(courseClass);
            }
        }
        return "redirect:/teacher/course/class/" + courseId;
    }

    @PostMapping("/course/class/remove-student")
    public String removeStudentFromClass(@RequestParam Long courseId,
                                         @RequestParam Long studentId,
                                         HttpSession session) {
        User user = requireTeacher(session);
        if (user == null) return "redirect:/login";
        Course course = ownedCourse(user, courseId);
        if (course != null) {
            CourseEnrollment enrollment = enrollmentMapper.findByStudentAndCourse(studentId, courseId);
            if (enrollment != null && enrollment.getClassId() != null) {
                courseClassMapper.decrementCount(enrollment.getClassId());
            }
            enrollmentMapper.delete(studentId, courseId);
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
        model.addAttribute("stats", courses.stream().map(course -> scoreService.teacherCourseStatistics(course.getId())).collect(Collectors.toList()));
        return "teacher/score_statistics";
    }

    @GetMapping("/score/export")
    public ResponseEntity<String> exportScores(@RequestParam Long courseId, HttpSession session) {
        User user = requireTeacher(session);
        if (user == null) return ResponseEntity.status(401).body("请先登录");
        Course course = ownedCourse(user, courseId);
        if (course == null) return ResponseEntity.status(403).body("无权访问该课程");
        Map<String, Object> stats = scoreService.teacherCourseStatistics(courseId);
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append("排名,学生,用户名,作业成绩,考试成绩,实训成绩,最终成绩,学习进度\n");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) stats.get("rows");
        for (Map<String, Object> row : rows) {
            @SuppressWarnings("unchecked")
            Map<String, Object> progress = (Map<String, Object>) row.get("learningProgress");
            csv.append(row.get("rank")).append(',')
                    .append(csv(row.get("studentName"))).append(',')
                    .append(csv(row.get("username"))).append(',')
                    .append(row.get("homeworkScore")).append(',')
                    .append(row.get("examScore")).append(',')
                    .append(row.get("practiceScore")).append(',')
                    .append(row.get("finalScore")).append(',')
                    .append(progress == null ? 0 : progress.get("overallProgress"))
                    .append('\n');
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"score-report-" + courseId + ".csv\"")
                .header("Content-Type", "text/csv;charset=UTF-8")
                .body(csv.toString());
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
        try {
            String text = raw.trim().replace('T', ' ');
            if (text.length() == 16) text += ":00";
            return Timestamp.valueOf(LocalDateTime.parse(text.replace(' ', 'T')));
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeCourseStatus(String status) {
        if ("draft".equals(status) || "closed".equals(status) || "archived".equals(status)) return status;
        return "active";
    }

    private String normalizeTaskStatus(String status) {
        if ("draft".equals(status) || "closed".equals(status) || "expired".equals(status) || "retracted".equals(status)) return status;
        return "published";
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

    private List<Map<String, Object>> classMemberViews(Long courseId) {
        return enrollmentMapper.findByCourseId(courseId).stream().map(enrollment -> {
            Map<String, Object> row = new HashMap<>();
            User student = userMapper.findById(enrollment.getStudentId());
            CourseClass courseClass = enrollment.getClassId() == null ? null : courseClassMapper.findById(enrollment.getClassId());
            row.put("studentId", enrollment.getStudentId());
            row.put("studentName", student == null ? "学生ID:" + enrollment.getStudentId() : (student.getName() == null ? student.getUsername() : student.getName()));
            row.put("username", student == null ? "-" : student.getUsername());
            row.put("className", courseClass == null ? "默认班级" : courseClass.getName());
            row.put("joinedAt", enrollment.getEnrolledAt());
            row.put("onlineMinutes", 0);
            return row;
        }).collect(Collectors.toList());
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

    private String csv(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private Double sumScores(String questionScores) {
        double total = 0D;
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<?, ?> scores = mapper.readValue(questionScores, Map.class);
            for (Object value : scores.values()) {
                if (value == null || String.valueOf(value).trim().isEmpty()) continue;
                total += Double.parseDouble(String.valueOf(value));
            }
        } catch (Exception e) {
            return null;
        }
        return total;
    }
}
