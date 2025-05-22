package com.example.demo.service.checkoutCommand;

import com.example.demo.model.order;
import com.example.demo.model.orderDetail;
import com.example.demo.model.users;
import com.example.demo.repository.PromotionRepository;

public class SavePromotionCommand implements CheckoutCommand {
	
	private final PromotionRepository promotionRepository;

    public SavePromotionCommand(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

	@Override
	public void execute(order order, users user) {
		// TODO Auto-generated method stub
		if (order.getPromotionID() != null)
		{
            promotionRepository.saveUserPromotion(order.getPromotionID(), user.getUserID());
        }
        for (orderDetail detail : order.getOrderDetails()) 
        {
            if (detail.getPromotionID() != null)
            {
                promotionRepository.saveUserPromotion(detail.getPromotionID(), user.getUserID());
            }
        }
		
	}
	

}
