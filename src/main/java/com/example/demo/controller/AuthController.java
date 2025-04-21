package com.example.demo.controller;

import com.example.demo.model.users;
import com.example.demo.model.role;
import com.example.demo.service.UserService;
import com.example.demo.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthenticationManager authenticationManager, 
                         UserService userService, 
                         JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody users user) {
        try {
            // Validate required fields
            if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Email is required");
            }
            if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Password is required");
            }
            if (user.getFullname() == null || user.getFullname().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Full name is required");
            }
            if (user.getPhoneNumber() == null || user.getPhoneNumber().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Phone number is required");
            }

            // Check if user already exists
            try {
                userService.findByEmailOrPhone(user.getEmail());
                return ResponseEntity.badRequest().body("Email already registered");
            } catch (UsernameNotFoundException e) {
                // Email not found, continue
            }

            try {
                userService.findByEmailOrPhone(user.getPhoneNumber());
                return ResponseEntity.badRequest().body("Phone number already registered");
            } catch (UsernameNotFoundException e) {
                // Phone number not found, continue
            }

            // Set default role for new user
            role defaultRole = new role();
            defaultRole.setRoleID("CUSTOMER"); // Set default role to CUSTOMER
            user.setRole(defaultRole);

            // Register user
            users savedUser = userService.registerUser(user);
            
            // Generate JWT token with role
            String jwtToken = jwtUtil.generateToken(savedUser.getEmail(), savedUser.getRole().getRoleID());
            
            // Return success response with token
            Map<String, Object> response = new HashMap<>();
            response.put("jwtToken", jwtToken);
            response.put("user", savedUser);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Registration failed: ", e);
            return ResponseEntity.badRequest().body("Registration failed: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        try {
            String emailOrPhone = loginRequest.get("emailOrPhone");
            String password = loginRequest.get("password");

            if (emailOrPhone == null || password == null) {
                return ResponseEntity.badRequest().body("Email/phone and password are required");
            }

            // Authenticate user
            users user = userService.authenticateUser(emailOrPhone, password);
            
            // Generate JWT token with role
            String jwtToken = jwtUtil.generateToken(user.getEmail(), user.getRole().getRoleID());
            
            // Return success response with token and redirect URL
            Map<String, Object> response = new HashMap<>();
            response.put("jwtToken", jwtToken);
            response.put("user", user);
            response.put("redirectUrl", "/cart"); // Default redirect to cart
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Login failed: ", e);
            return ResponseEntity.badRequest().body("Login failed: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        try {
            // Xóa authentication
            SecurityContextHolder.clearContext();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Logout error: ", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/success")
    public String handleLoginSuccess(Authentication authentication) {
        if (authentication != null && authentication.getAuthorities() != null) {
            if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_Owner"))) {
                return "redirect:/owner/dashboard";  // Chuyển đến trang dashboard của Owner
            } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_Staff"))) {
                return "redirect:/staff/dashboard";  // Chuyển đến trang dashboard của Staff
            }
        }
        return "redirect:/";  // Mặc định chuyển về trang chủ (cho Customer)
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/access-denied";
    }
} 