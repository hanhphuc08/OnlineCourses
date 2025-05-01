package com.example.demo.controller;

import java.lang.ProcessBuilder.Redirect;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.model.cart;
import com.example.demo.model.course;
import com.example.demo.model.users;
import com.example.demo.repository.CartRepository;
import com.example.demo.service.CartService;
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
	@Autowired
	private CartService cartService;
	
	
	@GetMapping("/cart")
	public String viewCart(Model model,@RequestParam(required = false) String success, @RequestParam(required = false) String error) {
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
            
            model.addAttribute("success", success);
            model.addAttribute("error", error);
            return "commons/cart";
        } catch (RuntimeException e) {
            logger.error("Error fetching cart for user: {}", e.getMessage());
            model.addAttribute("error", "Không thể tải giỏ hàng. Vui lòng thử lại!");
            return "error/500";
        }
	}
	@GetMapping("/cart/remove/{id}")
    public Object removeFromCart(@PathVariable("id") int cartId,
                                 @RequestParam(value = "ajax", defaultValue = "false") boolean isAjax,
                                 RedirectAttributes redirectAttributes) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
                logger.warn("User not authenticated when removing cart item ID {}", cartId);
                if (isAjax) {
                    return ResponseEntity.status(401).body("error: Vui lòng đăng nhập để xóa sản phẩm khỏi giỏ hàng!");
                }
                redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để xóa sản phẩm khỏi giỏ hàng!");
                return "redirect:/login";
            }

            String username = authentication.getName();
            users user = userService.findByEmailOrPhone(username);
            if (user == null) {
                logger.warn("User not found for username: {}", username);
                if (isAjax) {
                    return ResponseEntity.badRequest().body("error: Không tìm thấy thông tin người dùng!");
                }
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy thông tin người dùng!");
                return "redirect:/login";
            }

            
            Optional<cart> cartItemOpt = cartRepository.findById(cartId);
            if (!cartItemOpt.isPresent() || cartItemOpt.get().getUserID() != user.getUserID()) {
                logger.warn("Cart item ID {} not found or does not belong to user ID {}", cartId, user.getUserID());
                if (isAjax) {
                    return ResponseEntity.badRequest().body("error: Mục giỏ hàng không tồn tại hoặc không thuộc về bạn!");
                }
                redirectAttributes.addFlashAttribute("error", "Mục giỏ hàng không tồn tại hoặc không thuộc về bạn!");
                return "redirect:/cart";
            }

            
            cartService.removeFromCart(cartId);
            logger.info("Removed cart item ID {} for user ID {}", cartId, user.getUserID());

            if (isAjax) {
                return ResponseEntity.ok("success: Đã xóa sản phẩm khỏi giỏ hàng!");
            }
            redirectAttributes.addFlashAttribute("success", "Đã xóa sản phẩm khỏi giỏ hàng!");
            return "redirect:/cart";

        } catch (Exception e) {
            logger.error("Error removing cart item ID {}: {}", cartId, e.getMessage(), e);
            if (isAjax) {
                return ResponseEntity.badRequest().body("error: Không thể xóa sản phẩm khỏi giỏ hàng!");
            }
            redirectAttributes.addFlashAttribute("error", "Không thể xóa sản phẩm khỏi giỏ hàng!");
            return "redirect:/cart";
        }
    }
	
	@GetMapping("/cart/total")
    public ResponseEntity<String> getCartTotal() {
		try {
	        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
	        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
	            return ResponseEntity.status(401).body("$0.00");
	        }

	        String username = authentication.getName();
	        users user = userService.findByEmailOrPhone(username);
	        if (user == null) {
	            return ResponseEntity.badRequest().body("$0.00");
	        }

	        List<cart> cartItems = cartRepository.findByUserId(user.getUserID());
	        double total = cartItems.stream()
	                .mapToDouble(item -> {
	                    course course = courseService.getCourseById(item.getCourseID());
	                    return course != null ? course.getPrices().doubleValue() : 0.0;
	                })
	                .sum();

	        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
	        return ResponseEntity.ok(currencyFormat.format(total));
	    } catch (Exception e) {
	        logger.error("Error calculating cart total: {}", e.getMessage(), e);
	        return ResponseEntity.badRequest().body("$0.00");
	    }
	}

}
