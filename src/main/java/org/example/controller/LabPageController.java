package org.example.controller;

import org.example.model.User;
import org.example.util.SessionUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpSession;

@Controller
public class LabPageController {

    @GetMapping("/student/lab")
    public String studentLab(HttpSession session, Model model) {
        if (!SessionUtils.isLoggedIn(session) || !SessionUtils.isStudent(session)) return "redirect:/login";
        User user = new User();
        user.setUsername(SessionUtils.getUsername(session));
        user.setRole("student");
        model.addAttribute("user", user);
        return "lab";
    }

    @GetMapping("/teacher/lab")
    public String teacherLab(HttpSession session, Model model) {
        if (!SessionUtils.isLoggedIn(session) || !SessionUtils.isTeacher(session)) return "redirect:/login";
        User user = new User();
        user.setUsername(SessionUtils.getUsername(session));
        user.setRole("teacher");
        model.addAttribute("user", user);
        return "lab";
    }
}
