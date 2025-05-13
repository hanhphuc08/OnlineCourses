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

    public void sendVerificationEmail(String to, String verificationCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("lamngocnhaky8@gmail.com");
        message.setTo(to);
        message.setSubject("Xác thực email - Online Course");
        message.setText("Xin chào,\n\n"
                + "Cảm ơn bạn đã đăng ký tài khoản tại Online Course.\n"
                + "Mã xác thực của bạn là: " + verificationCode + "\n\n"
                + "Vui lòng sử dụng mã này để xác thực email của bạn.\n"
                + "Mã này sẽ hết hạn sau 1 phút.\n\n"
                + "Nếu bạn không thực hiện đăng ký này, vui lòng bỏ qua email này.\n\n"
                + "Trân trọng,\n"
                + "Online Course Team");
        
        emailSender.send(message);
    }
} 