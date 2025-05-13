package com.example.demo.controller;

import com.example.demo.model.users;
import com.example.demo.service.UserService;
import com.example.demo.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ForgotPasswordController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private EmailService emailService;

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "commons/forgotpassword";
    }

    @PostMapping("/api/auth/forgot-password")
    public ResponseEntity<String> processForgotPassword(@RequestParam("email") String email) {
        try {
            users user = userService.findByEmailOrPhone(email);
            
            if (user != null) {
                String resetCode = userService.generateResetCode();       
                userService.saveResetCode(user, resetCode);
                
                try {
                    emailService.sendResetPasswordEmail(user.getEmail(), resetCode);
                    return ResponseEntity.ok("success:Mã xác nhận đã được gửi đến email của bạn.");
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body("error:Không thể gửi email. Vui lòng thử lại sau.");
                }
            } else {
                return ResponseEntity.badRequest().body("error:Email hoặc số điện thoại không tồn tại trong hệ thống.");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("error:Có lỗi xảy ra. Vui lòng thử lại sau.");
        }
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm() {
        return "commons/reset-password";
    }

    @PostMapping("/api/auth/reset-password")
    public ResponseEntity<String> processResetPassword(@RequestParam("email") String email,
                                     @RequestParam("resetCode") String resetCode,
                                     @RequestParam("newPassword") String newPassword,
                                     @RequestParam("confirmPassword") String confirmPassword) {
        try {
            if (!newPassword.equals(confirmPassword)) {
                return ResponseEntity.badRequest().body("error:Mật khẩu xác nhận không khớp.");
            }

            if (!userService.verifyResetCode(email, resetCode)) {
                return ResponseEntity.badRequest().body("error:Mã xác nhận không đúng hoặc đã hết hạn (mã chỉ có hiệu lực trong 1 phút).");
            }

            userService.resetPassword(email, newPassword);
            return ResponseEntity.ok("success:Mật khẩu đã được đặt lại thành công. Vui lòng đăng nhập lại.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("error:Có lỗi xảy ra. Vui lòng thử lại sau.");
        }
    }
} 