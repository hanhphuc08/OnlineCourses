package com.example.demo.email;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

public abstract class BaseEmailSender {
	
	
	protected JavaMailSender mailSender;
	
	public BaseEmailSender(JavaMailSender mailSender)
	{
	    this.mailSender = mailSender;
	}
	
	public final void sendEmail() {
		if(isHtml()) {
			sendHtmlEmail();
		}
		else {
			sendPlainTextEmail();
		}
	}
	protected abstract String getTo();
	protected abstract String getSubject();
	protected abstract String getBody();
	
    protected boolean isHtml() {
        return false;
    }
    
    protected void sendPlainTextEmail() {
    	
    	SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("lamngocnhaky8@gmail.com");
        message.setTo(getTo());
        message.setSubject(getSubject());
        message.setText(getBody());
        mailSender.send(message);
    }
    
    protected void sendHtmlEmail() {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("lamngocnhaky8@gmail.com");
            helper.setTo(getTo());
            helper.setSubject(getSubject());
            helper.setText(getBody(), true);
            mailSender.send(message);
            
        } catch (MessagingException e) {
        	
            throw new RuntimeException("Lỗi gửi HTML email: " + e.getMessage(), e);
        }
    }
    
	

}
