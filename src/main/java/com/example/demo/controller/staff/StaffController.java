package com.example.demo.controller.staff;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;

@Controller
@RequestMapping("/staff")
@PreAuthorize("hasRole('Staff')")
public class StaffController {

    @GetMapping("/home")
    public String staffHome() {
        return "staff/home";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // TODO: Add staff dashboard statistics
        return "staff/dashboard";
    }

    @GetMapping("/courses")
    public String courses(Model model) {
        // TODO: Add course management logic
        return "staff/courses";
    }
} 