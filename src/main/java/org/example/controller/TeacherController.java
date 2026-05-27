package org.example.controller;

import org.example.model.*;
import org.example.util.MarkdownUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/teacher")
public class TeacherController {

  // 模拟数据库
  public static final List<Course> courseDatabase = new CopyOnWriteArrayList<>();
  public static final List<Task> taskDatabase = new CopyOnWriteArrayList<>();
  public static final List<Submission> submissionDatabase = new CopyOnWriteArrayList<>();

  // ========== 课程管理 ==========
  @GetMapping("/course/manage")
  public String manageCourses(HttpSession session, Model model,
      @RequestParam(required = false) Long courseId) {
    User user = (User) session.getAttribute("currentUser");
    if (user == null || !"teacher".equals(user.getRole())) {
      return "redirect:/login";
    }

    List<Course> myCourses = courseDatabase.stream()
        .filter(c -> user.getUsername().equals(c.getTeacherId()))
        .collect(Collectors.toList());

    model.addAttribute("courses", myCourses);

    // 如果有 courseId 参数，可以传给前端用于高亮或滚动
    if (courseId != null) {
      model.addAttribute("highlightCourseId", courseId);
    }

    return "teacher/course_manage";
  }

  @GetMapping("/course/create")
  public String showCreateCourse() {
    return "teacher/course_create";
  }

  @PostMapping("/course/create")
  public String createCourse(@RequestParam String courseName,
      @RequestParam String courseCode,
      @RequestParam Double credit,
      @RequestParam(required = false) String description,
      HttpSession session) {
    User user = (User) session.getAttribute("currentUser");
    Course c = new Course();
    c.setCourseId(System.currentTimeMillis());
    c.setCourseName(courseName);
    c.setCourseCode(courseCode);
    c.setCredit(BigDecimal.valueOf(credit));
    c.setTeacherId(user.getUsername());
    c.setDescription(description != null && !description.isEmpty() ? description : "暂无描述");
    c.setStatus("进行中");
    // 修改这一行：使用4个参数的构造方法
    c.getClasses().add(new CourseClass(System.currentTimeMillis(), c.getCourseId(), "默认班级", generateInviteCode()));
    courseDatabase.add(c);
    return "redirect:/teacher/course/manage";
  }

  @GetMapping("/course/edit/{id}")
  public String editCourse(@PathVariable Long id, Model model, HttpSession session) {
    User user = (User) session.getAttribute("currentUser");
    Course course = courseDatabase.stream()
        .filter(c -> c.getCourseId().equals(id) && user.getUsername().equals(c.getTeacherId()))
        .findFirst().orElse(null);
    model.addAttribute("course", course);
    return "teacher/course_edit";
  }

  @PostMapping("/course/update")
  public String updateCourse(@RequestParam Long courseId,
      @RequestParam String courseName,
      @RequestParam String courseCode,
      @RequestParam Double credit,
      @RequestParam String description,
      HttpSession session) {
    User user = (User) session.getAttribute("currentUser");
    Course course = courseDatabase.stream()
        .filter(c -> c.getCourseId().equals(courseId) && user.getUsername().equals(c.getTeacherId()))
        .findFirst().orElse(null);
    if (course != null) {
      course.setCourseName(courseName);
      course.setCourseCode(courseCode);
      course.setCredit(BigDecimal.valueOf(credit));
      course.setDescription(description);
    }
    return "redirect:/teacher/course/manage";
  }

  @GetMapping("/course/delete/{id}")
  public String deleteCourse(@PathVariable Long id, HttpSession session) {
    courseDatabase.removeIf(c -> c.getCourseId().equals(id));
    taskDatabase.removeIf(t -> t.getCourseId().equals(id));
    return "redirect:/teacher/course/manage";
  }

  // ========== 作业管理 ==========
  @GetMapping("/task/manage")
  public String manageTasks(HttpSession session, Model model) {
    User user = (User) session.getAttribute("currentUser");
    if (user == null || !"teacher".equals(user.getRole())) return "redirect:/login";

    List<Course> myCourses = courseDatabase.stream()
        .filter(c -> user.getUsername().equals(c.getTeacherId()))
        .collect(Collectors.toList());

    List<Long> courseIds = myCourses.stream().map(Course::getCourseId).collect(Collectors.toList());
    List<Task> myTasks = taskDatabase.stream()
        .filter(t -> courseIds.contains(t.getCourseId()))
        .collect(Collectors.toList());

    model.addAttribute("courses", myCourses);
    model.addAttribute("tasks", myTasks);
    return "teacher/task_manage";
  }

  @GetMapping("/task/create")
  public String showCreateTask(@RequestParam(required = false) Long courseId, Model model, HttpSession session) {
    User user = (User) session.getAttribute("currentUser");
    List<Course> myCourses = courseDatabase.stream()
        .filter(c -> user.getUsername().equals(c.getTeacherId()))
        .collect(Collectors.toList());
    model.addAttribute("courses", myCourses);
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
      @RequestParam(required = false) Double fullScore,
      HttpSession session) {
    User user = (User) session.getAttribute("currentUser");
    Task t = new Task();
    t.setTaskId(System.currentTimeMillis());
    t.setTitle(title);
    t.setCourseId(courseId);
    t.setTaskType(taskType);
    t.setDescription(description);
    t.setContent(content);
    t.setAllowedFileTypes(allowedFileTypes);
    t.setFullScore(fullScore != null ? BigDecimal.valueOf(fullScore) : BigDecimal.ZERO);
    t.setStartTime(new Date());
    t.setStatus("已发布");
    
    taskDatabase.add(t);
    return "redirect:/teacher/task/manage";
  }

  @GetMapping("/task/detail/{taskId}")
  public String taskDetail(@PathVariable Long taskId, Model model) {
    Task task = taskDatabase.stream().filter(t -> t.getTaskId().equals(taskId)).findFirst().orElse(null);
    List<Submission> submissions = submissionDatabase.stream()
        .filter(s -> s.getTaskId().equals(taskId))
        .collect(Collectors.toList());
    model.addAttribute("task", task);
    if (task != null) {
      model.addAttribute("taskContentHtml", MarkdownUtils.toHtml(task.getContent()));
    }
    model.addAttribute("submissions", submissions);
    return "teacher/task_detail";
  }

  @GetMapping("/task/grade/{submissionId}")
  public String showGradeForm(@PathVariable Long submissionId, Model model) {
    Submission submission = submissionDatabase.stream()
        .filter(s -> s.getSubmissionId().equals(submissionId))
        .findFirst().orElse(null);
    model.addAttribute("submission", submission);
    return "teacher/task_grade";
  }

  @PostMapping("/task/grade")
  public String submitGrade(@RequestParam Long submissionId,
      @RequestParam Double score,
      @RequestParam(required = false) String comment) {
    Submission submission = submissionDatabase.stream()
        .filter(s -> s.getSubmissionId().equals(submissionId))
        .findFirst().orElse(null);
    if (submission != null) {
      submission.setScore(BigDecimal.valueOf(score));
      submission.setComment(comment);
      submission.setSubmitStatus("已批改");
      submission.setGradedTime(new Date());
    }
    return "redirect:/teacher/task/manage";
  }

  @GetMapping("/task/delete")
  public String deleteTask(@RequestParam Long taskId) {
    taskDatabase.removeIf(t -> t.getTaskId().equals(taskId));
    submissionDatabase.removeIf(s -> s.getTaskId().equals(taskId));
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
    throw new RuntimeException("Could not read the file!");
  }

  // ========== 班级管理 ==========

  // 查看课程下的班级列表
  @GetMapping("/course/class/{courseId}")
  public String manageClasses(@PathVariable Long courseId, HttpSession session, Model model) {
    User user = (User) session.getAttribute("currentUser");
    if (user == null || !"teacher".equals(user.getRole())) {
      return "redirect:/login";
    }

    Course course = courseDatabase.stream()
        .filter(c -> c.getCourseId().equals(courseId) && user.getUsername().equals(c.getTeacherId()))
        .findFirst().orElse(null);

    if (course == null) {
      return "redirect:/teacher/course/manage";
    }

    model.addAttribute("course", course);
    model.addAttribute("classes", course.getClasses());
    return "teacher/class_manage";
  }

  // 创建班级
  @PostMapping("/course/class/create")
  public String createClass(@RequestParam Long courseId,
      @RequestParam String className,
      HttpSession session) {
    User user = (User) session.getAttribute("currentUser");
    Course course = courseDatabase.stream()
        .filter(c -> c.getCourseId().equals(courseId) && user.getUsername().equals(c.getTeacherId()))
        .findFirst().orElse(null);

    if (course != null) {
      String inviteCode = generateInviteCode();
      CourseClass newClass = new CourseClass(
          System.currentTimeMillis(),
          courseId,
          className,
          inviteCode
      );
      course.addClass(newClass);
    }

    return "redirect:/teacher/course/class/" + courseId;
  }

  // 删除班级
  @GetMapping("/course/class/delete")
  public String deleteClass(@RequestParam Long courseId, @RequestParam Long classId, HttpSession session) {
    User user = (User) session.getAttribute("currentUser");
    Course course = courseDatabase.stream()
        .filter(c -> c.getCourseId().equals(courseId) && user.getUsername().equals(c.getTeacherId()))
        .findFirst().orElse(null);

    if (course != null) {
      course.getClasses().removeIf(c -> c.getClassId().equals(classId));
    }

    return "redirect:/teacher/course/class/" + courseId;
  }

  // 生成邀请码
  private String generateInviteCode() {
    return "INV" + System.currentTimeMillis() + new Random().nextInt(1000);
  }

  // ========== 成绩统计（占位） ==========
  @GetMapping("/score/statistics")
  public String scoreStatistics(HttpSession session, Model model) {
    User user = (User) session.getAttribute("currentUser");
    List<Course> myCourses = courseDatabase.stream()
        .filter(c -> user.getUsername().equals(c.getTeacherId()))
        .collect(Collectors.toList());
    model.addAttribute("courses", myCourses);
    return "teacher/score_statistics";
  }
}

