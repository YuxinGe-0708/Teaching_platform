package org.example.controller;

import org.example.entity.Course;
import org.example.entity.CourseClass;
import org.example.entity.CourseEnrollment;
import org.example.entity.ExamRecord;
import org.example.entity.OperationLog;
import org.example.entity.StudyNote;
import org.example.entity.Submission;
import org.example.entity.Task;
import org.example.entity.User;
import org.example.mapper.CourseClassMapper;
import org.example.mapper.CourseEnrollmentMapper;
import org.example.mapper.ExamRecordMapper;
import org.example.mapper.OperationLogMapper;
import org.example.mapper.SubmissionMapper;
import org.example.mapper.TeachingResourceMapper;
import org.example.mapper.DiscussionMapper;
import org.example.mapper.StudyNoteMapper;
import org.example.mapper.UserMapper;
import org.example.service.AiService;
import org.example.service.CourseService;
import org.example.service.ExamService;
import org.example.service.ScoreService;
import org.example.service.TaskService;
import org.example.util.DownloadUtils;
import org.example.util.ExamContentUtils;
import org.example.util.MarkdownUtils;
import org.example.util.TaskMetadataUtils;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
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
    private final TeachingResourceMapper resourceMapper;
    private final ScoreService scoreService;
    private final DiscussionMapper discussionMapper;
    private final CourseEnrollmentMapper enrollmentMapper;
    private final CourseClassMapper courseClassMapper;
    private final UserMapper userMapper;
    private final StudyNoteMapper studyNoteMapper;
    private final AiService aiService;
    private final ExamService examService;
    private final ExamRecordMapper examRecordMapper;

    public StudentController(CourseService courseService,
                             TaskService taskService,
                             SubmissionMapper submissionMapper,
                             OperationLogMapper operationLogMapper,
                             TeachingResourceMapper resourceMapper,
                             ScoreService scoreService,
                             DiscussionMapper discussionMapper,
                             CourseEnrollmentMapper enrollmentMapper,
                             CourseClassMapper courseClassMapper,
                             UserMapper userMapper,
                             StudyNoteMapper studyNoteMapper,
                             AiService aiService,
                             ExamService examService,
                             ExamRecordMapper examRecordMapper) {
        this.courseService = courseService;
        this.taskService = taskService;
        this.submissionMapper = submissionMapper;
        this.operationLogMapper = operationLogMapper;
        this.resourceMapper = resourceMapper;
        this.scoreService = scoreService;
        this.discussionMapper = discussionMapper;
        this.enrollmentMapper = enrollmentMapper;
        this.courseClassMapper = courseClassMapper;
        this.userMapper = userMapper;
        this.studyNoteMapper = studyNoteMapper;
        this.aiService = aiService;
        this.examService = examService;
        this.examRecordMapper = examRecordMapper;
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

    @GetMapping("/classes")
    public String myClasses(HttpSession session, Model model) {
        User user = requireStudent(session);
        if (user == null) return "redirect:/login";
        model.addAttribute("user", user);
        model.addAttribute("classes", studentClassViews(user.getId()));
        return "student/class_manage";
    }

    @GetMapping("/tasks")
    public String taskLibrary(@RequestParam(required = false) String type, HttpSession session, Model model) {
        User user = requireStudent(session);
        if (user == null) return "redirect:/login";
        String activeType = type == null || type.trim().isEmpty() ? "all" : type.trim();
        List<Map<String, Object>> rows = new ArrayList<>();
        List<Submission> submissions = submissionMapper.findByStudentId(user.getId());
        Map<Long, Submission> byTask = submissions.stream()
                .filter(s -> s.getTaskId() != null)
                .collect(Collectors.toMap(Submission::getTaskId, s -> s, (a, b) -> a));
        Set<Long> addedTaskIds = new java.util.LinkedHashSet<>();
        for (Course course : courseService.getStudentCourses(user.getId())) {
            for (Task task : taskService.getCourseTasks(course.getId())) {
                if (task.getId() == null || !addedTaskIds.add(task.getId())) continue;
                if (!"published".equals(task.getStatus())) continue;
                if (!"all".equals(activeType) && !activeType.equals(task.getType())) continue;
                Map<String, Object> row = UserController.toTaskView(task);
                row.put("courseId", course.getId());
                row.put("courseName", course.getName());
                row.put("submission", byTask.get(task.getId()));
                row.put("completed", byTask.containsKey(task.getId()));
                rows.add(row);
            }
        }
        model.addAttribute("user", user);
        model.addAttribute("tasks", rows);
        model.addAttribute("type", activeType);
        return "student/task_library";
    }

    @GetMapping("/notes")
    public String notes(HttpSession session, Model model, @RequestParam(required = false) Long noteId) {
        User user = requireStudent(session);
        if (user == null) return "redirect:/login";
        StudyNote editing = null;
        if (noteId != null) {
            StudyNote found = studyNoteMapper.findById(noteId);
            if (found != null && user.getId().equals(found.getStudentId())) editing = found;
        }
        model.addAttribute("user", user);
        model.addAttribute("courses", UserController.toCourseViews(courseService.getStudentCourses(user.getId())));
        model.addAttribute("notes", studyNoteMapper.findByStudentId(user.getId()));
        model.addAttribute("editing", editing);
        return "student/study_notes";
    }

    @PostMapping("/notes/save")
    public String saveNote(@RequestParam(required = false) Long noteId,
                           @RequestParam Long courseId,
                           @RequestParam(required = false) Long resourceId,
                           @RequestParam String title,
                           @RequestParam String content,
                           HttpSession session) {
        User user = requireStudent(session);
        if (user == null) return "redirect:/login";
        if (!isEnrolled(user.getId(), courseId)) return "redirect:/student/notes";
        StudyNote note = noteId == null ? null : studyNoteMapper.findById(noteId);
        if (note == null || !user.getId().equals(note.getStudentId())) {
            note = new StudyNote();
            note.setStudentId(user.getId());
            fillNote(note, courseId, resourceId, title, content);
            studyNoteMapper.insert(note);
        } else {
            fillNote(note, courseId, resourceId, title, content);
            studyNoteMapper.update(note);
        }
        return "redirect:/student/notes";
    }

    @PostMapping("/notes/delete")
    public String deleteNote(@RequestParam Long noteId, HttpSession session) {
        User user = requireStudent(session);
        if (user == null) return "redirect:/login";
        studyNoteMapper.deleteByStudent(noteId, user.getId());
        return "redirect:/student/notes";
    }

    @GetMapping("/notes/mindmap/{noteId}")
    public String noteMindMap(@PathVariable Long noteId, HttpSession session, Model model) {
        User user = requireStudent(session);
        if (user == null) return "redirect:/login";
        StudyNote note = studyNoteMapper.findById(noteId);
        if (note == null || !user.getId().equals(note.getStudentId())) return "redirect:/student/notes";
        if (note.getMindMap() == null || note.getMindMap().trim().isEmpty()) {
            note.setMindMap(aiService.generateMindMap(note.getCourseName(), note.getTitle(), note.getContent()));
            studyNoteMapper.updateMindMap(note);
        }
        model.addAttribute("user", user);
        model.addAttribute("note", note);
        return "student/note_mindmap";
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

    @GetMapping("/scores")
    public String scoreSummary(HttpSession session, Model model) {
        User user = requireStudent(session);
        if (user == null) return "redirect:/login";
        model.addAttribute("user", user);
        model.addAttribute("courseScores", scoreService.studentScoreSummary(user.getId()));
        return "student/score_summary";
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
        if (!isEnrolled(user.getId(), courseId)) return "redirect:/student/course/my";
        String activeTab = (tab == null || tab.trim().isEmpty()) ? "home" : tab.trim();
        List<java.util.Map<String, Object>> tasks = taskService.getCourseTasks(courseId).stream()
                .filter(task -> "published".equals(task.getStatus()))
                .filter(task -> filterTask(activeTab, task))
                .map(UserController::toTaskView)
                .collect(Collectors.toList());
        model.addAttribute("user", user);
        model.addAttribute("course", UserController.toCourseView(course));
        model.addAttribute("tasks", tasks);
        model.addAttribute("resources", resourceMapper.findByCourseId(courseId));
        model.addAttribute("progress", scoreService.learningProgress(user.getId(), courseId));
        model.addAttribute("notes", studyNoteMapper.findByStudentAndCourse(user.getId(), courseId));
        if ("discussion".equals(activeTab)) {
            model.addAttribute("posts", discussionMapper.findPostsByCourseId(courseId));
        }
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
        if (course == null || !isEnrolled(user.getId(), course.getId())) return "redirect:/student/course/my";

        if ("exam".equals(task.getType())) {
            return "redirect:/student/exam/start?taskId=" + taskId;
        }

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
        if ("exam".equals(task.getType())) {
            return "redirect:/student/exam/start?taskId=" + taskId;
        }
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
        return DownloadUtils.attachment(filePath);
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

    @GetMapping("/exam/start")
    public String examStart(@RequestParam Long taskId, HttpSession session, Model model) {
        User user = requireStudent(session);
        if (user == null) return "redirect:/login";
        Task task = taskService.findById(taskId);
        if (task == null || !"exam".equals(task.getType())) return "redirect:/student/tasks";
        Course course = courseService.findById(task.getCourseId());
        if (course == null || !isEnrolled(user.getId(), course.getId())) return "redirect:/student/course/my";

        ExamRecord record = examService.getExamRecord(user.getId(), taskId);
        if (record != null && record.isSubmitted()) {
            model.addAttribute("error", "您已完成并提交了本次考试，不能重复参加。");
            model.addAttribute("user", user);
            model.addAttribute("task", UserController.toTaskView(task));
            model.addAttribute("course", UserController.toCourseView(course));
            model.addAttribute("taskContentHtml", MarkdownUtils.toHtml(TaskMetadataUtils.visibleMarkdown(task.getDescription())));
            model.addAttribute("record", record);
            return "student/exam_start";
        }
        if (record != null && record.isInProgress()) {
            return "redirect:/student/exam/take?taskId=" + taskId;
        }

        String visibleMarkdown = TaskMetadataUtils.visibleMarkdown(task.getDescription());
        model.addAttribute("user", user);
        model.addAttribute("task", UserController.toTaskView(task));
        model.addAttribute("course", UserController.toCourseView(course));
        model.addAttribute("taskContentHtml", MarkdownUtils.toHtml(visibleMarkdown));
        model.addAttribute("record", record);
        return "student/exam_start";
    }

    @PostMapping("/exam/begin")
    public String examBegin(@RequestParam Long taskId, HttpSession session, Model model) {
        User user = requireStudent(session);
        if (user == null) return "redirect:/login";
        Task task = taskService.findById(taskId);
        if (task == null || !"exam".equals(task.getType())) return "redirect:/student/tasks";
        Course course = courseService.findById(task.getCourseId());
        if (course == null || !isEnrolled(user.getId(), course.getId())) return "redirect:/student/course/my";

        ExamRecord record = examService.getExamRecord(user.getId(), taskId);
        if (record != null && record.isSubmitted()) {
            model.addAttribute("error", "您已完成并提交了本次考试，不能重复参加。");
            return examStart(taskId, session, model);
        }

        examService.beginExam(user.getId(), taskId);
        return "redirect:/student/exam/take?taskId=" + taskId;
    }

    @GetMapping("/exam/take")
    public String examTake(@RequestParam Long taskId, HttpSession session, Model model) {
        User user = requireStudent(session);
        if (user == null) return "redirect:/login";
        Task task = taskService.findById(taskId);
        if (task == null || !"exam".equals(task.getType())) return "redirect:/student/tasks";
        Course course = courseService.findById(task.getCourseId());
        if (course == null || !isEnrolled(user.getId(), course.getId())) return "redirect:/student/course/my";

        ExamRecord record = examService.getExamRecord(user.getId(), taskId);
        if (record == null) return "redirect:/student/exam/start?taskId=" + taskId;

        if (record.isSubmitted()) {
            model.addAttribute("user", user);
            model.addAttribute("task", UserController.toTaskView(task));
            model.addAttribute("course", UserController.toCourseView(course));
            model.addAttribute("record", record);
            model.addAttribute("alreadySubmitted", true);
            model.addAttribute("autoSubmitted", false);
            return "student/exam_take";
        }

        long remainingSeconds = examService.getRemainingSeconds(record, task);
        if (remainingSeconds <= 0 && record.isInProgress()) {
            record = examService.autoSubmitExam(user.getId(), taskId, record.getContent());
            examService.createSubmissionFromExam(record, task);
            model.addAttribute("user", user);
            model.addAttribute("task", UserController.toTaskView(task));
            model.addAttribute("course", UserController.toCourseView(course));
            model.addAttribute("record", record);
            model.addAttribute("alreadySubmitted", true);
            model.addAttribute("autoSubmitted", true);
            return "student/exam_take";
        }

        String visibleMarkdown = TaskMetadataUtils.visibleMarkdown(task.getDescription());
        model.addAttribute("user", user);
        model.addAttribute("task", UserController.toTaskView(task));
        model.addAttribute("course", UserController.toCourseView(course));
        model.addAttribute("taskContentHtml", MarkdownUtils.toHtml(visibleMarkdown));
        model.addAttribute("record", record);
        model.addAttribute("remainingSeconds", remainingSeconds);
        model.addAttribute("alreadySubmitted", false);
        return "student/exam_take";
    }

    @PostMapping("/exam/submit")
    public String examSubmit(@RequestParam Long taskId,
                             @RequestParam(required = false) String content,
                             @RequestParam(required = false) MultipartFile file,
                             @RequestParam(required = false, defaultValue = "1") String uploadQuestionId,
                             @RequestParam(required = false, defaultValue = "false") boolean auto,
                             HttpSession session,
                             Model model) {
        User user = requireStudent(session);
        if (user == null) return "redirect:/login";
        Task task = taskService.findById(taskId);
        if (task == null || !"exam".equals(task.getType())) return "redirect:/student/tasks";
        Course course = courseService.findById(task.getCourseId());
        if (course == null || !isEnrolled(user.getId(), course.getId())) return "redirect:/student/course/my";

        String answer = content == null ? "" : content;
        String submittedFilePath = saveFile(file);
        if (submittedFilePath != null) {
            answer = ExamContentUtils.addAttachment(answer, uploadQuestionId, submittedFilePath, originalName(file));
        }
        ExamRecord record;
        if (auto) {
            ExamRecord current = examService.getExamRecord(user.getId(), taskId);
            if (current != null && current.isSubmitted()) {
                record = current;
            } else {
                String currentContent = current != null ? current.getContent() : "";
                String finalContent = answer.isEmpty() ? currentContent : answer;
                record = examService.autoSubmitExam(user.getId(), taskId, finalContent);
            }
        } else {
            ExamRecord current = examService.getExamRecord(user.getId(), taskId);
            if (current != null && current.isSubmitted()) {
                record = current;
            } else {
                record = examService.submitExam(user.getId(), taskId, answer);
            }
        }

        if (record != null && !record.isSubmitted()) {
            record = examService.submitExam(user.getId(), taskId, answer);
        }

        examService.createSubmissionFromExam(record, task);
        log(user, "考试交卷", task.getTitle());

        model.addAttribute("user", user);
        model.addAttribute("task", UserController.toTaskView(task));
        model.addAttribute("course", UserController.toCourseView(course));
        model.addAttribute("record", record);
        model.addAttribute("alreadySubmitted", true);
        model.addAttribute("autoSubmitted", auto);
        return "student/exam_take";
    }

    @PostMapping("/exam/save")
    @ResponseBody
    public java.util.Map<String, Object> examSave(@RequestParam Long taskId,
                                                   @RequestParam(required = false) String content,
                                                   HttpSession session) {
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        User user = requireStudent(session);
        if (user == null) {
            result.put("code", 401);
            result.put("msg", "请先登录");
            return result;
        }
        Task task = taskService.findById(taskId);
        Course course = task == null ? null : courseService.findById(task.getCourseId());
        if (task == null || !"exam".equals(task.getType()) || course == null || !isEnrolled(user.getId(), course.getId())) {
            result.put("code", 403);
            result.put("msg", "无权暂存该考试");
            return result;
        }
        ExamRecord current = examService.getExamRecord(user.getId(), taskId);
        if (current == null || current.isSubmitted()) {
            result.put("code", 400);
            result.put("msg", "考试未开始或已提交，不能暂存");
            result.put("saved", false);
            return result;
        }
        ExamRecord record = examService.saveProgress(user.getId(), taskId, content);
        result.put("code", 200);
        result.put("msg", "已保存");
        result.put("saved", record != null && !record.isSubmitted());
        return result;
    }

    @PostMapping("/exam/upload")
    @ResponseBody
    public java.util.Map<String, Object> examUpload(@RequestParam Long taskId,
                                                    @RequestParam String questionId,
                                                    @RequestParam(required = false) String content,
                                                    @RequestParam("file") MultipartFile file,
                                                    HttpSession session) {
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        User user = requireStudent(session);
        if (user == null) {
            result.put("code", 401);
            result.put("msg", "请先登录");
            return result;
        }
        Task task = taskService.findById(taskId);
        Course course = task == null ? null : courseService.findById(task.getCourseId());
        if (task == null || !"exam".equals(task.getType()) || course == null || !isEnrolled(user.getId(), course.getId())) {
            result.put("code", 403);
            result.put("msg", "无权上传该考试附件");
            return result;
        }
        ExamRecord record = examService.getExamRecord(user.getId(), taskId);
        if (record == null || record.isSubmitted()) {
            result.put("code", 400);
            result.put("msg", "考试未开始或已提交，不能上传附件");
            return result;
        }
        String filePath = saveFile(file);
        if (filePath == null) {
            result.put("code", 400);
            result.put("msg", "附件保存失败，请重新选择文件");
            return result;
        }
        String baseContent = content == null || content.trim().isEmpty() ? record.getContent() : content;
        String updatedContent = ExamContentUtils.addAttachment(baseContent, questionId, filePath, originalName(file));
        examService.saveProgress(user.getId(), taskId, updatedContent);
        result.put("code", 200);
        result.put("msg", "上传成功");
        result.put("fileName", originalName(file));
        result.put("filePath", filePath);
        return result;
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

    private boolean isEnrolled(Long studentId, Long courseId) {
        return courseService.getStudentCourses(studentId).stream().anyMatch(course -> course.getId().equals(courseId));
    }

    private void fillNote(StudyNote note, Long courseId, Long resourceId, String title, String content) {
        note.setCourseId(courseId);
        note.setResourceId(resourceId);
        note.setTitle(title == null || title.trim().isEmpty() ? "未命名笔记" : title.trim());
        note.setContent(content == null ? "" : content.trim());
        note.setAiSummary("");
    }

    private List<Map<String, Object>> studentClassViews(Long studentId) {
        List<Map<String, Object>> views = new ArrayList<>();
        for (CourseEnrollment enrollment : enrollmentMapper.findByStudentId(studentId)) {
            Course course = courseService.findById(enrollment.getCourseId());
            if (course == null) continue;
            CourseClass courseClass = enrollment.getClassId() == null ? null : courseClassMapper.findById(enrollment.getClassId());
            if (courseClass == null) {
                List<CourseClass> classes = courseClassMapper.findByCourseId(course.getId());
                courseClass = classes.isEmpty() ? null : classes.get(0);
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("course", UserController.toCourseView(course));
            row.put("classInfo", courseClass);
            row.put("members", userMapper.findStudentsByCourseId(course.getId()));
            views.add(row);
        }
        return views;
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
            String filename = originalName(file);
            if (filename == null || filename.trim().isEmpty()) return null;
            filename = Paths.get(filename).getFileName().toString();
            Path uploadRoot = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            Files.createDirectories(uploadRoot);
            Path target = uploadRoot.resolve(System.currentTimeMillis() + "_" + filename).normalize();
            if (!target.startsWith(uploadRoot)) return null;
            Files.copy(file.getInputStream(), target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String originalName(MultipartFile file) {
        String filename = file == null ? "" : file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) return "attachment";
        return Paths.get(filename).getFileName().toString();
    }

    private boolean contains(String value, String lower) {
        return value != null && value.toLowerCase().contains(lower);
    }
}
