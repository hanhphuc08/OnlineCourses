package com.example.demo.email;

import org.springframework.mail.javamail.JavaMailSender;

public class ResetPasswordEmailSender extends BaseEmailSender {
	
	private final String to;
	private final String code;
	
	
	public ResetPasswordEmailSender(JavaMailSender mailSender, String to, String code) {
        super(mailSender);
        this.to = to;
        this.code = code;
    }

	@Override
	protected String getTo() {
		// TODO Auto-generated method stub
		return to;
	}

	@Override
	protected String getSubject() {
		// TODO Auto-generated method stub
		return "Đặt lại mật khẩu - Online Course";
	}

	@Override
	protected String getBody() {
		// TODO Auto-generated method stub
		return "Xin chào,\n\n"
        + "Mã xác nhận của bạn là: " + code + "\n"
        + "Mã này sẽ hết hạn sau 1 phút.\n\n"
        + "Trân trọng,\nOnline Course Team";
	}
	

}
