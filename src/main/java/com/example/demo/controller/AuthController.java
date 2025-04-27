package com.example.demo.controller;

import com.example.demo.model.users;
import com.example.demo.model.role;
import com.example.demo.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    private final AuthenticationManager authenticationManager;
    private final UserService userService;

    public AuthController(AuthenticationManager authenticationManager, 
                         UserService userService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
    }

    @PostMapping("/register")
    public String register(@ModelAttribute users user, RedirectAttributes redirectAttributes) {
        try {
            // Validate required fields
            if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Email is required");
                return "redirect:/register";
            }
            if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Password is required");
                return "redirect:/register";
            }
            if (user.getFullname() == null || user.getFullname().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Full name is required");
                return "redirect:/register";
            }
            if (user.getPhoneNumber() == null || user.getPhoneNumber().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Phone number is required");
                return "redirect:/register";
            }

            // Check if user already exists
            try {
                userService.findByEmailOrPhone(user.getEmail());
                redirectAttributes.addFlashAttribute("error", "Email already registered");
                return "redirect:/register";
            } catch (UsernameNotFoundException e) {
                // Email not found, continue
            }

            try {
                userService.findByEmailOrPhone(user.getPhoneNumber());
                redirectAttributes.addFlashAttribute("error", "Phone number already registered");
                return "redirect:/register";
            } catch (UsernameNotFoundException e) {
                // Phone number not found, continue
            }

            // Set default role for new user
            role defaultRole = new role();
            defaultRole.setRoleID("Customer"); // Sửa từ CUSTOMER thành Customer
            user.setRole(defaultRole);

            // Register user
            users savedUser = userService.registerUser(user);
            
            // Auto login after registration
            try {
                Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(savedUser.getEmail(), user.getPassword())
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.info("Auto login successful for new user: {} with role: {}", 
                    savedUser.getEmail(), 
                    savedUser.getRole().getRoleID());
            } catch (Exception e) {
                logger.error("Auto login failed for new user: {}", e.getMessage());
                // Continue with registration success even if auto login fails
            }
            
            redirectAttributes.addFlashAttribute("success", "Đăng ký thành công!");
            return "redirect:/";
            
        } catch (Exception e) {
            logger.error("Registration failed: ", e);
            redirectAttributes.addFlashAttribute("error", "Registration failed: " + e.getMessage());
            return "redirect:/register";
        }
    }

    @PostMapping("/login")
    public String login(@RequestParam String emailOrPhone, 
                       @RequestParam String password,
                       RedirectAttributes redirectAttributes) {
        try {
            if (emailOrPhone == null || password == null) {
                redirectAttributes.addFlashAttribute("error", "Email/phone and password are required");
                return "redirect:/login";
            }

            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(emailOrPhone, password)
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Log role information
            users user = userService.findByEmailOrPhone(emailOrPhone);
            logger.info("User {} logged in successfully with role: {}", 
                emailOrPhone, 
                user.getRole() != null ? user.getRole().getRoleID() : "no role");
                
            return "redirect:/";
            
        } catch (Exception e) {
            logger.error("Login failed: ", e);
            redirectAttributes.addFlashAttribute("error", "Login failed: " + e.getMessage());
            return "redirect:/login";
        }
    }

    @PostMapping("/logout")
    public String logout() {
        try {
            SecurityContextHolder.clearContext();
            return "redirect:/";
        } catch (Exception e) {
            logger.error("Logout error: ", e);
            return "redirect:/";
        }
    }

    @GetMapping("/success")
    public String handleLoginSuccess(Authentication authentication) {
        return "redirect:/";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/access-denied";
    }

    @GetMapping("/check-auth")
    @ResponseBody
    public ResponseEntity<?> checkAuth(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
} 