package com.example.demo.service.checkoutCommand;

import java.time.LocalDateTime;

import com.example.demo.model.order;
import com.example.demo.model.payment;
import com.example.demo.model.paymentMethod;
import com.example.demo.model.users;
import com.example.demo.repository.PaymentRepository;

public class SavePaymentCommand implements CheckoutCommand {
	
	private final PaymentRepository paymentRepository;

    public SavePaymentCommand(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

	

	@Override
	public void execute(order order, users user) {
		// TODO Auto-generated method stub
		payment payment = new payment();
        payment.setOrderID(order.getOrderID());
        payment.setCreateDate(LocalDateTime.now());
        payment.setPaymentStatus("Completed");
        payment.setCurrency("VND");
        payment.setAmount(order.getTotalAmount());

        paymentMethod paymentMethod = new paymentMethod();
        paymentMethod.setMethodType("EWallet");
        paymentMethod.seteWalletProvider("VNPAY");
        paymentMethod.seteWalletTransactionID(System.currentTimeMillis());
        payment.setPaymentMethod(paymentMethod);

        paymentRepository.save(payment);
	}

}
