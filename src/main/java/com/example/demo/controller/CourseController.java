	package com.example.demo.controller;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.model.course;
import com.example.demo.model.courseStatus;
import com.example.demo.model.order;
import com.example.demo.model.orderDetail;
import com.example.demo.model.users;
import com.example.demo.repository.CartRepository;
import com.example.demo.service.CourseService;
import com.example.demo.service.UserService;

import jakarta.servlet.http.HttpSession;

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
            
            if (course.getStatus() == null) {
                logger.warn("Course status is null for course ID: {}", courseId);
                course.setStatus(courseStatus.INACTIVE);
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
    public Object addToCart(@PathVariable("id") int courseId,
                            @RequestParam(value = "ajax", defaultValue = "false") boolean isAjax,
                            Model model, RedirectAttributes redirectAttributes) {
        try {
            course course = courseService.getCourseById(courseId);
            if (course == null) {
                logger.warn("Course with ID {} not found", courseId);
                if (isAjax) {
                    return ResponseEntity.badRequest().body("error: Khóa học không tồn tại!");
                }
                redirectAttributes.addFlashAttribute("error", "Khóa học không tồn tại!");
                return "redirect:/course/detail/" + courseId;
            }
            if (course.getStatus() == courseStatus.INACTIVE) {
                logger.warn("Cố gắng thêm khóa học không hoạt động ID {} vào giỏ hàng", courseId);
                if (isAjax) {
                    return ResponseEntity.badRequest().body("error: Khóa học hiện không hoạt động!");
                }
                redirectAttributes.addFlashAttribute("error", "Khóa học hiện không hoạt động!");
                return "redirect:/course/detail/" + courseId;
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
                logger.warn("User not authenticated when adding course ID {} to cart", courseId);
                if (isAjax) {
                    return ResponseEntity.status(401).body("error: Vui lòng đăng nhập để thêm khóa học vào giỏ hàng!");
                }
                redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để thêm khóa học vào giỏ hàng!");
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

            int userId = user.getUserID();
            try {
                cartRepository.addToCart(userId, courseId, 1);
                logger.info("Added course ID {} to cart for user ID {}", courseId, userId);
                if (isAjax) {
                    return ResponseEntity.ok("success: Đã thêm khóa học vào giỏ hàng!");
                }
                redirectAttributes.addFlashAttribute("success", "Đã thêm khóa học vào giỏ hàng!");
                return "redirect:/cart";
            } catch (RuntimeException e) {
                logger.warn("Course ID {} already in cart for user ID {}", courseId, userId);
                if (isAjax) {
                    return ResponseEntity.badRequest().body("error: Khóa học đã có trong giỏ hàng!");
                }
                redirectAttributes.addFlashAttribute("error", "Khóa học đã có trong giỏ hàng!");
                return "redirect:/course/detail/" + courseId;
            }

        } catch (RuntimeException e) {
            logger.error("Error adding course ID {} to cart: {}", courseId, e.getMessage());
            if (isAjax) {
                return ResponseEntity.badRequest().body("error: " + e.getMessage());
            }
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/course/detail/" + courseId;
        }
    }

    @PostMapping("/checkout/{id}")
    public String checkout(@PathVariable("id") int courseId, HttpSession session, Authentication authentication, RedirectAttributes redirectAttributes) {
    	try {
            if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
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
                logger.warn("Khóa học với ID {} không tồn tại", courseId);
                redirectAttributes.addFlashAttribute("error", "Khóa học không tồn tại!");
                return "redirect:/course/detail/" + courseId;
            }
            if (course.getStatus() == courseStatus.INACTIVE) {
                logger.warn("Cố gắng thanh toán khóa học không hoạt động ID {}", courseId);
                redirectAttributes.addFlashAttribute("error", "Khóa học hiện không hoạt động!");
                return "redirect:/course/detail/" + courseId;
            }

            order order = new order();
            order.setUserID(user.getUserID());
            order.setOrderDate(java.time.LocalDateTime.now());
            order.setOrderStatus("TEMP");

            orderDetail detail = new orderDetail();
            detail.setCourseID(courseId);
            detail.setPrice(course.getPrices());
            detail.setCourse(course);
            List<orderDetail> orderDetails = new ArrayList<>();
            orderDetails.add(detail);
            order.setOrderDetails(orderDetails);
            order.setTotalAmount(course.getPrices());

            session.setAttribute("checkoutOrder", order);
            logger.info("Đã tạo order tạm thời cho userId: {}, courseId: {} từ trang chi tiết khóa học", user.getUserID(), courseId);
            return "redirect:/checkout";
        } catch (RuntimeException e) {
            logger.error("Lỗi khi xử lý mua ngay từ trang chi tiết khóa học: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/course/detail/" + courseId;
        }
    }
}
