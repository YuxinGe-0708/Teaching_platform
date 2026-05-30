package org.example.controller;

import org.example.entity.Course;
import org.example.entity.Submission;
import org.example.entity.Task;
import org.example.entity.User;
import org.example.service.CourseService;
import org.example.service.TaskService;
import org.example.service.UserService;
import org.example.util.TaskMetadataUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class UserController {

    private final UserService userService;
    private final CourseService courseService;
    private final TaskService taskService;

    public UserController(UserService userService, CourseService courseService, TaskService taskService) {
        this.userService = userService;
        this.courseService = courseService;
        this.taskService = taskService;
    }

    @GetMapping("/")
    public String showIndexPage(HttpSession session, Model model) {
        model.addAttribute("user", session.getAttribute("currentUser"));
        return "index";
    }

    @GetMapping("/register")
    public String showRegisterForm() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam String username,
                               @RequestParam String password,
                               @RequestParam String role,
                               Model model) {
        String normalizedUsername = username == null ? "" : username.trim();
        String normalizedRole = role == null ? "" : role.trim().toLowerCase();
        model.addAttribute("username", normalizedUsername);
        model.addAttribute("role", normalizedRole);
        if (normalizedUsername.length() < 3 || normalizedUsername.length() > 20) {
            model.addAttribute("error", "用户名长度必须为 3-20 个字符");
            return "register";
        }
        if (password == null || password.trim().length() < 6 || password.trim().length() > 32) {
            model.addAttribute("error", "密码长度必须为 6-32 个字符");
            return "register";
        }
        if (!"student".equals(normalizedRole) && !"teacher".equals(normalizedRole)) {
            model.addAttribute("error", "身份选择无效");
            return "register";
        }
        User user = userService.register(normalizedUsername, password, normalizedRole, normalizedUsername);
        if (user == null) {
            model.addAttribute("error", "用户名已存在");
            return "register";
        }
        return "redirect:/login?registered=1";
    }

    @GetMapping("/login")
    public String showLoginForm(@RequestParam(value = "registered", required = false) String registered, Model model) {
        if ("1".equals(registered)) {
            model.addAttribute("success", "注册成功，请登录。");
        }
        return "login";
    }

    @PostMapping("/login")
    public String loginUser(@RequestParam String username,
                            @RequestParam String password,
                            HttpSession session,
                            Model model) {
        User user = userService.login(username == null ? "" : username.trim(), password);
        if (user == null) {
            model.addAttribute("error", "用户名或密码错误");
            return "login";
        }
        session.setAttribute("currentUser", user);
        return "redirect:/home";
    }

    @GetMapping("/home")
    public String showHomePage(HttpSession session, Model model) {
        User currentUser = requireUser(session);
        model.addAttribute("user", currentUser);
        if (currentUser == null) {
            model.addAttribute("courses", Collections.emptyList());
            return "home";
        }
        if ("teacher".equals(currentUser.getRole())) {
            model.addAttribute("courses", toCourseViews(courseService.getTeacherCourses(currentUser.getId())));
        } else {
            model.addAttribute("courses", toCourseViews(courseService.getStudentCourses(currentUser.getId())));
        }
        return "home";
    }

    @GetMapping("/profile")
    public String showProfilePage(HttpSession session, Model model) {
        User currentUser = requireUser(session);
        if (currentUser == null) return "redirect:/login";
        model.addAttribute("user", currentUser);
        if ("student".equals(currentUser.getRole())) {
            model.addAttribute("courses", toCourseViews(courseService.getStudentCourses(currentUser.getId())));
            model.addAttribute("submissions", taskService.getStudentSubmissions(currentUser.getId()));
        } else {
            model.addAttribute("courses", toCourseViews(courseService.getTeacherCourses(currentUser.getId())));
            model.addAttribute("submissions", Collections.<Submission>emptyList());
        }
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam(required = false) String name,
                                @RequestParam(required = false) String realName,
                                @RequestParam(required = false) String nickname,
                                @RequestParam(required = false) String email,
                                HttpSession session) {
        User currentUser = requireUser(session);
        if (currentUser == null) return "redirect:/login";
        currentUser.setName(firstNonBlank(name, realName, nickname, currentUser.getName(), currentUser.getUsername()));
        currentUser.setEmail(email);
        userService.updateProfile(currentUser);
        session.setAttribute("currentUser", currentUser);
        return "redirect:/profile?updated=1";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    static User requireUser(HttpSession session) {
        Object user = session.getAttribute("currentUser");
        return user instanceof User ? (User) user : null;
    }

    static List<Map<String, Object>> toCourseViews(List<Course> courses) {
        return courses.stream().map(UserController::toCourseView).collect(Collectors.toList());
    }

    static Map<String, Object> toCourseView(Course course) {
        Map<String, Object> view = new HashMap<>();
        view.put("courseId", course.getId());
        view.put("courseName", course.getName());
        view.put("courseCode", course.getCode());
        view.put("credit", course.getCredits());
        view.put("teacherId", course.getTeacherName() == null ? course.getTeacherId() : course.getTeacherName());
        view.put("description", course.getDescription());
        view.put("studentCount", course.getStudentCount() == null ? 0 : course.getStudentCount());
        view.put("status", "active".equals(course.getStatus()) ? "进行中" : course.getStatus());
        view.put("inviteCode", course.getInviteCode());
        Map<String, Object> clazz = new HashMap<>();
        clazz.put("classId", course.getId());
        clazz.put("courseId", course.getId());
        clazz.put("className", "默认班级");
        clazz.put("inviteCode", course.getInviteCode());
        clazz.put("currentCount", course.getStudentCount() == null ? 0 : course.getStudentCount());
        clazz.put("maxCount", 100);
        view.put("classes", Collections.singletonList(clazz));
        return view;
    }

    static Map<String, Object> toTaskView(Task task) {
        String visible = TaskMetadataUtils.visibleMarkdown(task.getDescription());
        Map<String, Object> view = new HashMap<>();
        view.put("taskId", task.getId());
        view.put("title", task.getTitle());
        view.put("courseId", task.getCourseId());
        view.put("courseName", task.getCourseName());
        view.put("taskKind", task.getType());
        view.put("taskType", displayTaskType(task.getType()));
        view.put("description", visible);
        view.put("content", visible);
        view.put("fullScore", task.getMaxScore());
        view.put("startTime", task.getCreatedAt() == null ? new Date() : new Date(task.getCreatedAt().getTime()));
        view.put("endTime", task.getEndTime() == null ? null : new Date(task.getEndTime().getTime()));
        view.put("status", "published".equals(task.getStatus()) ? "已发布" : task.getStatus());
        view.put("allowedFileTypes", "");
        view.put("testCasesJson", TaskMetadataUtils.testCasesJson(task.getDescription()));
        view.put("hasExamAnswer", !TaskMetadataUtils.examAnswer(task.getDescription()).trim().isEmpty());
        return view;
    }

    static String dbTaskType(String type) {
        if ("exam".equals(type) || "考试".equals(type)) return "exam";
        if ("programming".equals(type) || "编程实训".equals(type)) return "programming";
        return "homework";
    }

    static String displayTaskType(String type) {
        if ("exam".equals(type)) return "考试";
        if ("programming".equals(type)) return "编程实训";
        return "作业";
    }

    static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }
}
