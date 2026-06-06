package org.example.controller;

import org.example.entity.Notification;
import org.example.entity.OperationLog;
import org.example.entity.User;
import org.example.mapper.CourseMapper;
import org.example.mapper.NotificationMapper;
import org.example.mapper.OperationLogMapper;
import org.example.mapper.SubmissionMapper;
import org.example.mapper.TaskMapper;
import org.example.mapper.UserMapper;
import org.example.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final CourseMapper courseMapper;
    private final TaskMapper taskMapper;
    private final SubmissionMapper submissionMapper;
    private final NotificationMapper notificationMapper;
    private final OperationLogMapper operationLogMapper;

    public AdminController(UserService userService,
                           UserMapper userMapper,
                           CourseMapper courseMapper,
                           TaskMapper taskMapper,
                           SubmissionMapper submissionMapper,
                           NotificationMapper notificationMapper,
                           OperationLogMapper operationLogMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.courseMapper = courseMapper;
        this.taskMapper = taskMapper;
        this.submissionMapper = submissionMapper;
        this.notificationMapper = notificationMapper;
        this.operationLogMapper = operationLogMapper;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/login";
        model.addAttribute("user", admin);
        model.addAttribute("stats", dashboardStats());
        model.addAttribute("logs", operationLogMapper.findRecent());
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String users(@RequestParam(required = false) String role,
                        @RequestParam(required = false) String message,
                        HttpSession session,
                        Model model) {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/login";
        model.addAttribute("user", admin);
        model.addAttribute("users", userService.listUsers(role));
        model.addAttribute("role", role == null ? "" : role);
        model.addAttribute("message", message);
        return "admin/user_manage";
    }

    @PostMapping("/users/update")
    public String updateUser(@RequestParam Long userId,
                             @RequestParam(required = false) String name,
                             @RequestParam(required = false) String email,
                             @RequestParam String role,
                             HttpSession session) {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/login";
        boolean ok = userService.updateByAdmin(userId, name, email, role);
        log(admin, "管理员修改用户", "userId=" + userId + ", role=" + role);
        return "redirect:/admin/users?message=" + (ok ? "updated" : "failed");
    }

    @PostMapping("/users/reset-password")
    public String resetPassword(@RequestParam Long userId,
                                @RequestParam String password,
                                HttpSession session) {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/login";
        boolean ok = userService.resetPassword(userId, password);
        log(admin, "管理员重置密码", "userId=" + userId);
        return "redirect:/admin/users?message=" + (ok ? "passwordReset" : "failed");
    }

    @PostMapping("/users/delete")
    public String deleteUser(@RequestParam Long userId, HttpSession session) {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/login";
        if (admin.getId().equals(userId)) {
            return "redirect:/admin/users?message=selfDeleteBlocked";
        }
        boolean ok = userService.deleteUser(userId);
        log(admin, "管理员删除用户", "userId=" + userId);
        return "redirect:/admin/users?message=" + (ok ? "deleted" : "failed");
    }

    @GetMapping("/notifications")
    public String notifications(@RequestParam(required = false) String message,
                                HttpSession session,
                                Model model) {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/login";
        model.addAttribute("user", admin);
        model.addAttribute("notifications", notificationMapper.findRecent());
        model.addAttribute("message", message);
        return "admin/notification_manage";
    }

    @PostMapping("/notifications/publish")
    public String publishNotification(@RequestParam String title,
                                      @RequestParam String content,
                                      @RequestParam(defaultValue = "system") String type,
                                      @RequestParam(required = false) String targetRole,
                                      HttpSession session) {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/login";
        List<User> targets = userService.listUsers(targetRole);
        for (User target : targets) {
            Notification notification = new Notification();
            notification.setUserId(target.getId());
            notification.setTitle(title);
            notification.setContent(content);
            notification.setType(type);
            notificationMapper.insert(notification);
        }
        log(admin, "管理员发布公告", title);
        return "redirect:/admin/notifications?message=published";
    }

    @PostMapping("/notifications/delete")
    public String deleteNotification(@RequestParam Long notificationId, HttpSession session) {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/login";
        notificationMapper.deleteById(notificationId);
        log(admin, "管理员删除公告", "notificationId=" + notificationId);
        return "redirect:/admin/notifications?message=deleted";
    }

    @GetMapping("/logs")
    public String logs(HttpSession session, Model model) {
        User admin = requireAdmin(session);
        if (admin == null) return "redirect:/login";
        List<OperationLog> logs = operationLogMapper.findRecent();
        model.addAttribute("user", admin);
        model.addAttribute("logs", logs);
        return "admin/logs";
    }

    private User requireAdmin(HttpSession session) {
        User user = UserController.requireUser(session);
        return user != null && "admin".equals(user.getRole()) ? user : null;
    }

    private Map<String, Object> dashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("userCount", userMapper.countAll());
        stats.put("studentCount", userMapper.countByRole("student"));
        stats.put("teacherCount", userMapper.countByRole("teacher"));
        stats.put("adminCount", userMapper.countByRole("admin"));
        stats.put("courseCount", courseMapper.countAll());
        stats.put("taskCount", taskMapper.countAll());
        stats.put("submissionCount", submissionMapper.countAll());
        return stats;
    }

    private void log(User admin, String action, String detail) {
        OperationLog log = new OperationLog();
        log.setUserId(admin.getId());
        log.setUsername(admin.getUsername());
        log.setAction(action);
        log.setDetail(detail);
        operationLogMapper.insert(log);
    }
}
