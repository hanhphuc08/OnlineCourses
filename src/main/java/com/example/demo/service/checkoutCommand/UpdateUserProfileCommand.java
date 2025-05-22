package com.example.demo.service.checkoutCommand;

import com.example.demo.model.order;
import com.example.demo.model.users;
import com.example.demo.repository.UserRepository;

public class UpdateUserProfileCommand implements CheckoutCommand {
	
	private final UserRepository userRepository;

    public UpdateUserProfileCommand(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

	@Override
	public void execute(order order, users user) {
		// TODO Auto-generated method stub
		userRepository.updateUserProfile(user.getUserID(), user.getFullname(), user.getEmail(), user.getPhoneNumber());
	}
	
	

}
