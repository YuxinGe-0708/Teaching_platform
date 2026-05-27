package org.example.controller;

import org.example.model.User;
import org.example.util.SessionUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpSession;

@Controller
public class AiPageController {

    @GetMapping("/student/ai")
    public String studentAi(HttpSession session, Model model) {
        if (!SessionUtils.isLoggedIn(session) || !SessionUtils.isStudent(session)) return "redirect:/login";
        User user = new User();
        user.setUsername(SessionUtils.getUsername(session));
        user.setRole("student");
        model.addAttribute("user", user);
        return "ai_assistant";
    }

    @GetMapping("/teacher/ai")
    public String teacherAi(HttpSession session, Model model) {
        if (!SessionUtils.isLoggedIn(session) || !SessionUtils.isTeacher(session)) return "redirect:/login";
        User user = new User();
        user.setUsername(SessionUtils.getUsername(session));
        user.setRole("teacher");
        model.addAttribute("user", user);
        return "ai_assistant";
    }
}
