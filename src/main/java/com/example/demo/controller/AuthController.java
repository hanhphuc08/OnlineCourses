package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {
    
    @GetMapping("/login")
    public String login() {
        return "commons/login";
    }
    
    @GetMapping("/register")
    public String register() {
        return "commons/register";
    }
    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "commons/forgotpassword";
    }
} 