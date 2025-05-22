package com.example.demo.service.checkoutCommand;

import com.example.demo.model.order;
import com.example.demo.model.users;

public interface CheckoutCommand {
	
	void execute(order order, users user);

}
