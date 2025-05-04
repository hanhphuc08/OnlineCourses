package com.example.demo.controller.owner;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;

@Controller
@RequestMapping("/owner")
@PreAuthorize("hasRole('Owner')")
public class OwnerController {

    @GetMapping("/home")
    public String ownerHome() {
        return "owner/home";
    }

    @GetMapping("/owner/dashboard")
    public String dashboard(Model model) {
        // TODO: Add dashboard statistics
        return "owner/dashboard";
    }

    @GetMapping("/users")
    public String userManagement(Model model) {
        // TODO: Add user management logic
        return "owner/users";
    }

    @GetMapping("/courses")
    public String courseManagement(Model model) {
        // TODO: Add course management logic
        return "owner/courses";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        // TODO: Add settings management
        return "owner/settings";
    }
} 