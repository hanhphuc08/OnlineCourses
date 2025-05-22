package com.example.demo.email;

import com.example.demo.model.users;

import java.text.DecimalFormat;

import org.springframework.mail.javamail.JavaMailSender;

import com.example.demo.model.order;
import com.example.demo.model.orderDetail;

public class OrderConfirmationEmailSender extends BaseEmailSender {
	
	 private final users user;
	    private final order order;

	    public OrderConfirmationEmailSender(JavaMailSender mailSender, users user, order order)
	    {
	        super(mailSender);
	        this.user = user;
	        this.order = order;
	    }
	protected String getTo() {
		// TODO Auto-generated method stub
		return user.getEmail();
	}
	@Override
	protected String getSubject() {
		// TODO Auto-generated method stub
		return "Xác nhận thanh toán - Đơn hàng #" + order.getOrderID();
	}
	
	 @Override
	    protected boolean isHtml() {
	        return true; // Kích hoạt gửi HTML
	    }

	    @Override
	    protected String getBody() {
	        StringBuilder sb = new StringBuilder();
	        DecimalFormat formatter = new DecimalFormat("#,###");

	        sb.append("<div style='font-family: Arial, sans-serif;'>");
	        sb.append("<h2 style='color: green;'>Xác nhận thanh toán thành công</h2>");
	        sb.append("<p>Kính chào <strong>").append(user.getFullname()).append("</strong>,</p>");
	        sb.append("<p>Cảm ơn bạn đã đăng ký khóa học. Dưới đây là chi tiết đơn hàng của bạn:</p>");
	        sb.append("<ul>");

	        for (orderDetail detail : order.getOrderDetails()) {
	            String price = formatter.format(detail.getPrice());
	            sb.append("<li>")
	              .append(detail.getCourse().getTitle())
	              .append(" - ").append(price).append(" VND</li>");
	        }

	        String total = formatter.format(order.getTotalAmount());

	        sb.append("</ul>");
	        sb.append("<p><strong>Tổng cộng: ").append(total).append(" VND</strong></p>");
	        sb.append("<p>Trân trọng,<br>Đội ngũ hỗ trợ Online Course</p>");
	        sb.append("</div>");

	        return sb.toString();
	    }
    
    

}
