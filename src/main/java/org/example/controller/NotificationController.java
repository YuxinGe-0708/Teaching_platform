package org.example.controller;

import org.example.entity.Notification;
import org.example.entity.User;
import org.example.mapper.NotificationMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
public class NotificationController {

    private final NotificationMapper notificationMapper;

    public NotificationController(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @GetMapping("/notifications")
    public String notifications(HttpSession session, Model model) {
        User user = UserController.requireUser(session);
        if (user == null) return "redirect:/login";
        List<Notification> notifications = notificationMapper.findByUserId(user.getId());
        model.addAttribute("user", user);
        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", notificationMapper.countUnread(user.getId()));
        return "notifications";
    }

    @PostMapping("/notifications/read")
    public String markAsRead(@RequestParam Long notificationId, HttpSession session) {
        User user = UserController.requireUser(session);
        if (user == null) return "redirect:/login";
        notificationMapper.markAsReadForUser(notificationId, user.getId());
        return "redirect:/notifications";
    }

    @PostMapping("/notifications/read-all")
    public String markAllAsRead(HttpSession session) {
        User user = UserController.requireUser(session);
        if (user == null) return "redirect:/login";
        notificationMapper.markAllAsRead(user.getId());
        return "redirect:/notifications";
    }
}
