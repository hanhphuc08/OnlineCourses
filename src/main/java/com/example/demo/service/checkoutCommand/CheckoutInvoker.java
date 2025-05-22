package com.example.demo.service.checkoutCommand;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.demo.model.order;
import com.example.demo.model.users;

public class CheckoutInvoker {
	private static final Logger logger = LoggerFactory.getLogger(CheckoutInvoker.class);
	
	private final List<CheckoutCommand> commands = new ArrayList<>();

    public void addCommand(CheckoutCommand command) {
        commands.add(command);
    }

    public void executeAll(order order, users user) {
    	
    	for (CheckoutCommand command : commands)
    	{
            try {
                command.execute(order, user);
            } catch (Exception e)
            {
                logger.error("Lỗi khi thực thi lệnh {}: {}", command.getClass().getSimpleName(), e.getMessage(), e);
                throw new RuntimeException("Lỗi trong quá trình thanh toán: " + e.getMessage(), e);
            }
        }
    }

}
