package com.example.demo.email;

import org.springframework.mail.javamail.JavaMailSender;

public class VerificationEmailSender extends BaseEmailSender {
	
	private final String to;
    private final String code;

    public VerificationEmailSender(JavaMailSender mailSender, String to, String code)
    {
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
		return "Xác thực email - Online Course";
	}

	@Override
	protected String getBody() {
		// TODO Auto-generated method stub
		return "Xin chào,\n\n"
                + "Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản của mình.\n"
                + "Mã xác nhận của bạn là: " + code + "\n\n"
                + "Vui lòng sử dụng mã này để đặt lại mật khẩu của bạn.\n"
                + "Mã này sẽ hết hạn sau 1 phút.\n\n"
                + "Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.\n\n"
                + "Trân trọng,\n"
                + "Online Course Team";
	}
	
	

}
