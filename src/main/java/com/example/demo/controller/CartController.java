package com.example.demo.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.cart;
import com.example.demo.model.course;
import com.example.demo.model.users;
import com.example.demo.repository.CartRepository;
import com.example.demo.service.CourseService;
import com.example.demo.service.UserService;

@Controller
public class CartController {
	private static final Logger logger = LoggerFactory.getLogger(CartController.class);
	
	@Autowired
    private CourseService courseService;
	@Autowired
	private UserService userService;
	@Autowired
	private CartRepository cartRepository;
	
	
	@GetMapping("/cart")
	public String viewCart(Model model) {
		try {
           
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
                logger.warn("User not authenticated when viewing cart");
                model.addAttribute("error", "Vui lòng đăng nhập để xem giỏ hàng!");
                return "login";
            }

            String username = authentication.getName();
            users user = userService.findByEmailOrPhone(username);
            int userId = user.getUserID();

         
            List<cart> cartItems = cartRepository.findByUserId(userId);
            List<cart> enrichedCartItems = cartItems.stream().map(cart -> {
            	course course = courseService.getCourseById(cart.getCourseID());
                if (course != null) {
                    cart.setCourse(course);
                } else {
                    logger.warn("Course with ID {} not found for cart item {}", cart.getCourseID(), cart.getCartID());
                }
                return cart;
            }).collect(Collectors.toList());

            
            double total = enrichedCartItems.stream()
                .mapToDouble(item -> item.getCourse().getPrices().doubleValue())
                .sum();

            model.addAttribute("cartItems", enrichedCartItems);
            model.addAttribute("total", total);
            return "commons/cart";
        } catch (RuntimeException e) {
            logger.error("Error fetching cart for user: {}", e.getMessage());
            model.addAttribute("error", "Không thể tải giỏ hàng. Vui lòng thử lại!");
            return "error/500";
        }
	}
	

}
