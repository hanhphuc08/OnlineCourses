package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender emailSender;

    public void sendResetPasswordEmail(String to, String resetCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("lamngocnhaky8@gmail.com");
        message.setTo(to);
        message.setSubject("Đặt lại mật khẩu - Online Course");
        message.setText("Xin chào,\n\n"
                + "Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản của mình.\n"
                + "Mã xác nhận của bạn là: " + resetCode + "\n\n"
                + "Vui lòng sử dụng mã này để đặt lại mật khẩu của bạn.\n"
                + "Mã này sẽ hết hạn sau 1 phút.\n\n"
                + "Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.\n\n"
                + "Trân trọng,\n"
                + "Online Course Team");
        
        emailSender.send(message);
    }
} 