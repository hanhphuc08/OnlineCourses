package com.example.demo.controller;

import com.example.demo.model.users;
import com.example.demo.service.UserService;
import com.example.demo.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("email") String email, 
                                      Model model, 
                                      RedirectAttributes redirectAttributes) {
        try {
            users user = userService.findByEmailOrPhone(email);
            
            if (user != null) {
                String resetCode = userService.generateResetCode();       
                userService.saveResetCode(user, resetCode);
                
                try {
                    emailService.sendResetPasswordEmail(user.getEmail(), resetCode);
                    redirectAttributes.addFlashAttribute("email", email);
                    redirectAttributes.addFlashAttribute("success", "Mã xác nhận đã được gửi đến email của bạn.");
                    return "redirect:/reset-password";
                } catch (Exception e) {
                    redirectAttributes.addFlashAttribute("alert", "Không thể gửi email. Vui lòng thử lại sau.");
                    return "redirect:/forgot-password";
                }
            } else {
                redirectAttributes.addFlashAttribute("alert", "Email hoặc số điện thoại không tồn tại trong hệ thống.");
                return "redirect:/forgot-password";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("alert", "Có lỗi xảy ra. Vui lòng thử lại sau.");
            return "redirect:/forgot-password";
        }
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(Model model) {
        if (!model.containsAttribute("email")) {
            return "redirect:/forgot-password";
        }
        return "commons/reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("email") String email,
                                     @RequestParam("resetCode") String resetCode,
                                     @RequestParam("newPassword") String newPassword,
                                     @RequestParam("confirmPassword") String confirmPassword,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        try {
            // Kiểm tra mật khẩu khớp nhau
            if (!newPassword.equals(confirmPassword)) {
                model.addAttribute("alert", "Mật khẩu xác nhận không khớp.");
                model.addAttribute("email", email);
                return "commons/reset-password";
            }

            if (!userService.verifyResetCode(email, resetCode)) {
                model.addAttribute("alert", "Mã xác nhận không đúng hoặc đã hết hạn (mã chỉ có hiệu lực trong 1 phút).");
                model.addAttribute("email", email);
                return "commons/reset-password";
            }

            userService.resetPassword(email, newPassword);
            redirectAttributes.addFlashAttribute("success", "Mật khẩu đã được đặt lại thành công. Vui lòng đăng nhập lại.");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("alert", "Có lỗi xảy ra. Vui lòng thử lại sau.");
            model.addAttribute("email", email);
            return "commons/reset-password";
        }
    }
} 