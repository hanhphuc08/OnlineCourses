package com.example.demo.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.demo.model.course;
import com.example.demo.model.order;
import com.example.demo.model.users;
import com.example.demo.repository.OrderRepository;
import com.example.demo.service.CourseService;
import com.example.demo.service.UserService;

@Controller
public class ProfileController {

	private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CourseService courseService;

	@GetMapping("/profile")
	public String showProfile(Model model, Authentication authentication) {
		if (authentication == null) {
            logger.info("Chưa đăng nhập, chuyển hướng đến /login");
            return "redirect:/login";
        }

        String email = authentication.getName();
        logger.info("Xử lý trang profile cho email: {}", email);
        users user = userService.findByEmailOrPhone(email);
        if (user == null) {
            logger.warn("Không tìm thấy người dùng với email: {}", email);
            return "redirect:/login";
        }

        

        model.addAttribute("user", user);
        
        return "commons/profile";
	}

}
