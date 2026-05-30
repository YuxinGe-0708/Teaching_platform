package org.example.controller;

import org.example.entity.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpSession;

@Controller
public class LabPageController {

    @GetMapping("/student/lab")
    public String studentLab(HttpSession session, Model model) {
        User user = UserController.requireUser(session);
        if (user == null || !"student".equals(user.getRole())) return "redirect:/login";
        model.addAttribute("user", user);
        return "lab";
    }

    @GetMapping("/teacher/lab")
    public String teacherLab(HttpSession session, Model model) {
        User user = UserController.requireUser(session);
        if (user == null || !"teacher".equals(user.getRole())) return "redirect:/login";
        model.addAttribute("user", user);
        return "lab";
    }
}
