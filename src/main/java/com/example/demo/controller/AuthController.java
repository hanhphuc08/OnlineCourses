package com.example.demo.controller;

import com.example.demo.model.users;
import com.example.demo.service.FirebaseAuthService;
import com.example.demo.service.UserService;
import com.example.demo.util.JwtUtil;
import com.google.firebase.auth.FirebaseAuthException;
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
    private final FirebaseAuthService firebaseAuthService;

    public AuthController(AuthenticationManager authenticationManager, 
                         UserService userService, 
                         JwtUtil jwtUtil,
                         FirebaseAuthService firebaseAuthService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.firebaseAuthService = firebaseAuthService;
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

            // Register user with Firebase
            Map<String, Object> response = firebaseAuthService.registerUser(user);
            
            // Return success response with tokens
            return ResponseEntity.ok(response);
        } catch (FirebaseAuthException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
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
            
            // Log user information including role
            logger.info("User authenticated: {}, Role: {}", user.getEmail(), 
                user.getRole() != null ? user.getRole().getRoleID() : "No role");

            // Generate tokens
            String jwtToken = jwtUtil.generateToken(user.getEmail());
            String firebaseToken = firebaseAuthService.createCustomToken(String.valueOf(user.getUserID()));

            // Return response with complete user info including role
            Map<String, Object> response = new HashMap<>();
            response.put("uid", user.getUserID());
            response.put("jwtToken", jwtToken);
            response.put("firebaseToken", firebaseToken);
            response.put("user", user);  // This should include the role information
            
            // Add role information separately for clarity
            if (user.getRole() != null) {
                response.put("role", user.getRole());
            }

            return ResponseEntity.ok(response);
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.badRequest().body("Invalid credentials");
        } catch (Exception e) {
            logger.error("Login error: ", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/verify-token")
    public ResponseEntity<?> verifyToken(@RequestBody Map<String, String> tokenRequest) {
        String idToken = tokenRequest.get("idToken");
        try {
            // Xác thực Firebase ID token
            UserDetails userDetails = firebaseAuthService.verifyIdToken(idToken);
            
            // Lấy thông tin người dùng từ database
            users user = userService.findByEmailOrPhone(userDetails.getUsername());
            
            // Tạo JWT token mới
            String jwtToken = jwtUtil.generateToken(user.getEmail());
            
            // Kết hợp kết quả
            Map<String, Object> response = new HashMap<>();
            response.put("uid", userDetails.getUsername());
            response.put("jwtToken", jwtToken);
            response.put("user", user);
            
            return ResponseEntity.ok(response);
        } catch (FirebaseAuthException e) {
            return ResponseEntity.badRequest().body("Invalid token");
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