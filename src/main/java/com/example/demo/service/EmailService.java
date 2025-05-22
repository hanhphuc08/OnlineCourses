package com.example.demo.service;
import com.example.demo.email.*;
import org.slf4j.Logger;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.example.demo.model.order;
import com.example.demo.model.orderDetail;
import com.example.demo.model.users;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
	private static final Logger logger = LoggerFactory.getLogger(EmailService.class);


	@Autowired
    private JavaMailSender mailSender;

    public void sendResetPassword(String to, String code) {
        BaseEmailSender sender = new ResetPasswordEmailSender(mailSender, to, code);
        sender.sendEmail();
    }

    public void sendVerification(String to, String code) {
        BaseEmailSender sender = new VerificationEmailSender(mailSender, to, code);
        sender.sendEmail();
    }

    public void sendOrderConfirmation(users user, order order) {
        BaseEmailSender sender = new OrderConfirmationEmailSender(mailSender, user, order);
        sender.sendEmail();
    }
} 