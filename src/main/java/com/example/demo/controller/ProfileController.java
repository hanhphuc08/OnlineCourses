package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.demo.model.users;
import com.example.demo.service.UserService;

@Controller
public class ProfileController {

	@Autowired
	private UserService userService;

	@GetMapping("/profile")
	public String showProfile(Model model, Authentication authentication) {
		if (authentication != null && authentication.isAuthenticated()) {
			UserDetails userDetails = (UserDetails) authentication.getPrincipal();
			String username = userDetails.getUsername();
			
			// Lấy thông tin user từ database
			users user = userService.findByEmailOrPhone(username);
			if (user != null) {
				model.addAttribute("user", user);
			}
		}
		return "commons/profile";
	}

}
