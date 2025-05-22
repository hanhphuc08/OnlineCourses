package com.example.demo.service.checkoutCommand;

import com.example.demo.model.order;
import com.example.demo.model.users;
import com.example.demo.repository.OrderRepository;

public class SaveOrderCommand implements CheckoutCommand {
	
	private final OrderRepository orderRepository;

    public SaveOrderCommand(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

	@Override
	public void execute(order order, users user) {
		// TODO Auto-generated method stub
		 order.setOrderStatus("PENDING");
	        orderRepository.save(order);
		
	}
	

}
