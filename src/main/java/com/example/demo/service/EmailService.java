package com.example.demo.service;

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

    public void sendOrderConfirmationEmail(users user, order order) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(user.getEmail());
            helper.setSubject("Xác nhận thanh toán thành công - Đơn hàng #" + order.getOrderID());
            helper.setText(buildEmailContent(user, order), true); // true = HTML content

            emailSender.send(message);
            logger.info("Đã gửi email xác nhận đến {} cho đơn hàng {}", user.getEmail(), order.getOrderID());
        } catch (MessagingException e) {
            logger.error("Lỗi khi gửi email xác nhận đến {}: {}", user.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Không thể gửi email xác nhận: " + e.getMessage());
        }
    }

    private String buildEmailContent(users user, order order) {
        StringBuilder content = new StringBuilder();
        content.append("<h2>Xác nhận thanh toán thành công</h2>");
        content.append("<p>Kính chào ").append(user.getFullname()).append(",</p>");
        content.append("<p>Thanh toán của bạn cho đơn hàng #").append(order.getOrderID())
               .append(" đã thành công và sẽ được xử lý nhanh chóng.</p>");
        content.append("<h3>Chi tiết đơn hàng</h3>");
        content.append("<ul>");
        for (orderDetail detail : order.getOrderDetails()) {
            content.append("<li>")
                   .append(detail.getCourse().getTitle())
                   .append(" - Giá: ").append(detail.getPrice()).append(" VND")
                   .append("</li>");
        }
        content.append("</ul>");
        content.append("<p><strong>Tổng cộng: ").append(order.getTotalAmount()).append(" VND</strong></p>");
        content.append("<p>Cảm ơn bạn đã mua hàng! Nếu có thắc mắc, vui lòng liên hệ chúng tôi.</p>");
        content.append("<p>Trân trọng,<br>Đội ngũ hỗ trợ</p>");
        return content.toString();
    }
} 