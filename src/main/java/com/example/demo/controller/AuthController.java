package com.example.demo.controller;

import com.example.demo.model.users;
import com.example.demo.model.role;
import com.example.demo.service.UserService;
import com.example.demo.service.EmailService;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public AuthController(AuthenticationManager authenticationManager, 
                         UserService userService,
                         PasswordEncoder passwordEncoder,
                         EmailService emailService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@ModelAttribute users user) {
        try {
            if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("error:Email không được để trống");
            }
            if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("error:Mật khẩu không được để trống");
            }
            if (user.getFullname() == null || user.getFullname().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("error:Họ tên không được để trống");
            }
            if (user.getPhoneNumber() == null || user.getPhoneNumber().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("error:Số điện thoại không được để trống");
            }
            if (user.getGender() == null || user.getGender().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("error:Giới tính không được để trống");
            }
            try {
                userService.findByEmailOrPhone(user.getEmail());
                return ResponseEntity.badRequest().body("error:Email đã được sử dụng");
            } catch (UsernameNotFoundException e) {
            }

            try {
                userService.findByEmailOrPhone(user.getPhoneNumber());
                return ResponseEntity.badRequest().body("error:Số điện thoại đã được sử dụng");
            } catch (UsernameNotFoundException e) {
            }
            role defaultRole = new role();
            defaultRole.setRoleID("Customer");
            user.setRole(defaultRole);
            user.setStatus(0); // Inactive until email verification
            user.setCreateDate(LocalDateTime.now());
            user.setUpdateDate(LocalDateTime.now());
            String verificationCode = userService.generateVerificationCode();
            user.setEmailCode(verificationCode);
            users savedUser = userService.registerUser(user);
            
            try {
                emailService.sendVerificationEmail(savedUser.getEmail(), verificationCode);
                return ResponseEntity.ok("success:" + savedUser.getEmail());
            } catch (Exception e) {
                userService.deleteUser(savedUser.getUserID());
                return ResponseEntity.badRequest().body("error:Không thể gửi email xác thực. Vui lòng thử lại sau.");
            }
            
        } catch (Exception e) {
            logger.error("Registration failed: ", e);
            return ResponseEntity.badRequest().body("error:Có lỗi xảy ra. Vui lòng thử lại sau.");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam String emailOrPhone,
                                        @RequestParam String password,
                                        RedirectAttributes redirectAttributes) {
        try {
            if (emailOrPhone == null || password == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("{\"status\":\"error\",\"message\":\"Email/phone và mật khẩu không được để trống\"}");
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(emailOrPhone, password)
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            users user = userService.findByEmailOrPhone(emailOrPhone);
            logger.info("User {} logged in successfully with role: {}",
                    emailOrPhone,
                    user.getRole() != null ? user.getRole().getRoleID() : "no role");

            return ResponseEntity.ok("{\"status\":\"success\",\"message\":\"Đăng nhập thành công! Chào mừng bạn quay trở lại.\"}");

        } catch (Exception e) {
            logger.error("Login failed: ", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"status\":\"error\",\"message\":\"Email/phone hoặc mật khẩu không đúng\"}");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        try {
            SecurityContextHolder.clearContext();
            return ResponseEntity.ok("success:Bạn đã đăng xuất thành công!");
        } catch (Exception e) {
            logger.error("Logout error: ", e);
            return ResponseEntity.badRequest().body("error:Đã xảy ra lỗi khi đăng xuất");
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

    @PostMapping("/api/auth/register")
    public ResponseEntity<String> register(@RequestParam("email") String email,
                                         @RequestParam("phoneNumber") String phoneNumber,
                                         @RequestParam("fullname") String fullname,
                                         @RequestParam("password") String password,
                                         @RequestParam("gender") String gender) {
        try {
            // Validate required fields
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("error:Email không được để trống");
            }
            if (password == null || password.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("error:Mật khẩu không được để trống");
            }
            if (fullname == null || fullname.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("error:Họ tên không được để trống");
            }
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("error:Số điện thoại không được để trống");
            }
            if (gender == null || gender.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("error:Giới tính không được để trống");
            }

            // Check if user exists
            try {
                userService.findByEmailOrPhone(email);
                return ResponseEntity.badRequest().body("error:Email đã được sử dụng");
            } catch (UsernameNotFoundException e) {
                // Email not found, continue
            }

            try {
                userService.findByEmailOrPhone(phoneNumber);
                return ResponseEntity.badRequest().body("error:Số điện thoại đã được sử dụng");
            } catch (UsernameNotFoundException e) {
                // Phone number not found, continue
            }

            // Create new user
            users user = new users();
            user.setEmail(email);
            user.setPhoneNumber(phoneNumber);
            user.setFullname(fullname);
            user.setPassword(password);
            user.setGender(gender);
            role defaultRole = new role();
            defaultRole.setRoleID("Customer");
            user.setRole(defaultRole);
            user.setStatus(0); 
            user.setCreateDate(LocalDateTime.now());
            user.setUpdateDate(LocalDateTime.now());
            String verificationCode = userService.generateVerificationCode();
            user.setEmailCode(verificationCode);
            users savedUser = userService.registerUser(user);
            try {
                emailService.sendVerificationEmail(savedUser.getEmail(), verificationCode);
                return ResponseEntity.ok("success:Đăng ký thành công! Vui lòng kiểm tra email để xác thực tài khoản.");
            } catch (Exception e) {
             
                userService.deleteUser(savedUser.getUserID());
                return ResponseEntity.badRequest().body("error:Không thể gửi email xác thực. Vui lòng thử lại sau.");
            }
        } catch (Exception e) {
            logger.error("Registration failed: ", e);
            return ResponseEntity.badRequest().body("error:Có lỗi xảy ra. Vui lòng thử lại sau.");
        }
    }

    @GetMapping("/verify-email")
    public String showVerificationPage(@RequestParam(required = false) String email, Model model) {
        if (email == null || email.isEmpty()) {
            return "redirect:/register";
        }
        model.addAttribute("email", email);
        return "commons/verify-email";
    }

    @PostMapping("/verify-email")
    @ResponseBody
    public ResponseEntity<String> verifyEmail(@RequestParam String email, @RequestParam String code) {
        try {
            logger.info("Verifying email: {} with code: {}", email, code);
            users user = userService.findByEmailOrPhone(email);
            if (user == null) {
                logger.error("User not found for email: {}", email);
                return ResponseEntity.badRequest().body("error:Không tìm thấy người dùng với email này");
            }
            if (user.getStatus() == 1) {
                logger.error("Email already verified: {}", email);
                return ResponseEntity.badRequest().body("error:Email đã được xác thực trước đó");
            }
            if (user.getEmailCode() == null || !user.getEmailCode().equals(code)) {
                logger.error("Invalid verification code for email: {}", email);
                return ResponseEntity.badRequest().body("error:Mã xác thực không đúng");
            }
            user.setStatus(1);
            user.setEmailCode(null);
            user.setUpdateDate(LocalDateTime.now());
            userService.saveUser(user);
            logger.info("Email verified successfully: {}", email);
            return ResponseEntity.ok("success:Email đã được xác thực thành công");
        } catch (Exception e) {
            logger.error("Verification failed for email: " + email, e);
            return ResponseEntity.badRequest().body("error:Đã xảy ra lỗi khi xác thực");
        }
    }

    @PostMapping("/api/auth/resend-verification")
    public ResponseEntity<String> resendVerification(@RequestParam("email") String email) {
        try {
            users user = userService.findByEmailOrPhone(email);
            if (user == null) {
                return ResponseEntity.badRequest().body("error:Email không tồn tại.");
            }

            if (user.getStatus() == 1) {
                return ResponseEntity.badRequest().body("error:Email đã được xác thực trước đó.");
            }
            String verificationCode = userService.generateVerificationCode();
            user.setEmailCode(verificationCode);
            user.setUpdateDate(LocalDateTime.now());
            userService.saveUser(user);
            try {
                emailService.sendVerificationEmail(user.getEmail(), verificationCode);
                return ResponseEntity.ok("success:Mã xác thực mới đã được gửi đến email của bạn.");
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("error:Không thể gửi email xác thực. Vui lòng thử lại sau.");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("error:Có lỗi xảy ra. Vui lòng thử lại sau.");
        }
    }
} 