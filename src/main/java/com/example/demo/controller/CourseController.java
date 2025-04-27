package com.example.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.model.course;
import com.example.demo.model.users;
import com.example.demo.repository.CartRepository;
import com.example.demo.service.CourseService;
import com.example.demo.service.UserService;

@Controller
@RequestMapping("course")
public class CourseController {
	
	private static final Logger logger = LoggerFactory.getLogger(CourseController.class);
	
	@Autowired
    private CourseService courseService;
	@Autowired
	private UserService userService;
	@Autowired
	private CartRepository cartRepository;
	
	@GetMapping("/detail/{id}")
    public String getCourseById(@PathVariable("id") int courseId, Model model) {
        try {
            course course = courseService.getCourseById(courseId);
            if(course == null) {
            	logger.warn("Course with ID {} not found", courseId);
                return "error/404";
            }
            model.addAttribute("course", course);
            logger.info("Loaded course details for ID {}: {}", courseId, course.getTitle());
            return "category/productdetail";
        } catch (RuntimeException e) {
            logger.error("Error fetching course with ID {}: {}", courseId, e.getMessage());
            return "error/500";
        }
    }

	@PostMapping("/cart/add/{id}")
    public String addToCart(@PathVariable("id") int courseId, Model model, RedirectAttributes redirectAttributes) {
		try {
			course course = courseService.getCourseById(courseId);
			if(course == null) {
				logger.warn("Course with ID {} not found", courseId);
				return "error/404";
			}
			
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			if(authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
				logger.warn("User not authenticated when adding course ID {} to cart", courseId);
				redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để thêm khóa học vào giỏ hàng!");
				return "redirect:/login";
			}

			String username = authentication.getName();
			users user = userService.findByEmailOrPhone(username);
			
			if (user == null) {
				logger.warn("User not found for username: {}", username);
				redirectAttributes.addFlashAttribute("error", "Không tìm thấy thông tin người dùng!");
				return "redirect:/login";
			}

			int userId = user.getUserID();
			cartRepository.addToCart(userId, courseId, 1);
			logger.info("Added course ID {} to cart for user ID {}", courseId, userId);
			redirectAttributes.addFlashAttribute("success", "Đã thêm khóa học vào giỏ hàng!");
			
			return "redirect:/cart";
			
		} catch (RuntimeException e) {
			logger.error("Error adding course ID {} to cart: {}", courseId, e.getMessage());
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/course/detail/" + courseId;
		}
	}

    @PostMapping("/checkout/{id}")
    public String checkout(@PathVariable("id") int courseId, Model model, RedirectAttributes redirectAttributes) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if(authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
                logger.warn("User not authenticated when checking out course ID {}", courseId);
                redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để thanh toán!");
                return "redirect:/login";
            }

            String username = authentication.getName();
            users user = userService.findByEmailOrPhone(username);
            
            if (user == null) {
                logger.warn("User not found for username: {}", username);
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy thông tin người dùng!");
                return "redirect:/login";
            }

            course course = courseService.getCourseById(courseId);
            if (course == null) {
                return "error/404";
            }

            model.addAttribute("course", course);
            model.addAttribute("user", user);
            return "checkout";
            
        } catch (RuntimeException e) {
            logger.error("Error during checkout for course ID {}: {}", courseId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/course/detail/" + courseId;
        }
    }
}
