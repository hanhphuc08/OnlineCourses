package com.example.demo.service.checkoutCommand;

import com.example.demo.model.order;
import com.example.demo.model.users;
import com.example.demo.service.EmailService;

public class SendConfirmationEmailCommand implements CheckoutCommand {
	
	private final EmailService emailService;

    public SendConfirmationEmailCommand(EmailService emailService) {
        this.emailService = emailService;
    }

	@Override
	public void execute(order order, users user) {
		// TODO Auto-generated method stub
		emailService.sendOrderConfirmation(user, order);
	}
	

}
