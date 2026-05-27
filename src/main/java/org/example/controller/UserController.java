package org.example.controller;

import org.example.model.Course;
import org.example.model.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class UserController {

    // 内存模拟数据库
    private final Map<String, User> userDatabase = new HashMap<>();
    private static final Set<String> ALLOWED_ROLES = new HashSet<>(Arrays.asList("student", "teacher"));

    @GetMapping("/")
    public String showIndexPage(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        model.addAttribute("user", currentUser);
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

        if (normalizedUsername.isEmpty() || password == null || password.trim().isEmpty()) {
            model.addAttribute("error", "用户名和密码不能为空！");
            return "register";
        }
        if (!ALLOWED_ROLES.contains(normalizedRole)) {
            model.addAttribute("error", "身份选择无效！");
            return "register";
        }
        if (userDatabase.containsKey(normalizedUsername)) {
            model.addAttribute("error", "用户名已存在！");
            return "register";
        }

        userDatabase.put(normalizedUsername, new User(normalizedUsername, password, normalizedRole));
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
        User user = userDatabase.get(username.trim());
        if (user != null && user.getPassword().equals(password)) {
            session.setAttribute("currentUser", user);
            // 登录后直接跳到首页，首页会根据角色显示课程
            return "redirect:/home";
        }
        model.addAttribute("error", "用户名或密码错误！");
        return "login";
    }

    @GetMapping("/home")
    public String showHomePage(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        model.addAttribute("user", currentUser);

        // 数据共享逻辑：从 TeacherController 获取课程数据
        if (currentUser != null && "teacher".equals(currentUser.getRole())) {
            // 教师：看到自己创建的课
            List<Course> myCourses = TeacherController.courseDatabase.stream()
                .filter(c -> currentUser.getUsername().equals(c.getTeacherId()))
                .collect(Collectors.toList());
            model.addAttribute("courses", myCourses);
        } else if (currentUser != null && "student".equals(currentUser.getRole())) {
            // 学生：只看到自己选过的课
            List<Long> myCourseIds = StudentController.studentCourses.getOrDefault(currentUser.getUsername(), new ArrayList<>());
            List<Course> myCourses = TeacherController.courseDatabase.stream()
                .filter(c -> myCourseIds.contains(c.getCourseId()))
                .collect(Collectors.toList());
            model.addAttribute("courses", myCourses);
        } else {
            // 未登录：看到全平台所有的课
            model.addAttribute("courses", TeacherController.courseDatabase);
        }

        return "home";
    }

    @GetMapping("/profile")
    public String showProfilePage(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", currentUser);
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) String idNumber,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String school,
            @RequestParam(required = false) String major,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String motto,
            HttpSession session, Model model) {
            
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        // Basic validation
        if (email != null && !email.isEmpty() && !email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            model.addAttribute("error", "邮箱格式不合法！");
            model.addAttribute("user", currentUser);
            return "profile";
        }

        // Update fields
        currentUser.setNickname(nickname);
        currentUser.setRealName(realName);
        currentUser.setIdNumber(idNumber);
        currentUser.setEmail(email);
        currentUser.setSchool(school);
        currentUser.setMajor(major);
        currentUser.setClassName(className);
        currentUser.setGender(gender);
        currentUser.setMotto(motto);
        
        // Update in database (in-memory)
        userDatabase.put(currentUser.getUsername(), currentUser);
        session.setAttribute("currentUser", currentUser);

        model.addAttribute("user", currentUser);
        model.addAttribute("success", "个人信息修改成功！");
        return "profile";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}