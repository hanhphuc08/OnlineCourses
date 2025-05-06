package com.example.demo.controller.staff;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.repository.OrderRepository;
import com.example.demo.service.CourseService;
import com.example.demo.service.OrderService;
import com.example.demo.service.UserService;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;

@Controller
@RequestMapping("/staff")
@PreAuthorize("hasRole('Staff')")
public class StaffController {
	
	 private static final Logger logger = LoggerFactory.getLogger(StaffController.class);
	 
	 	@Autowired
	    private CourseService courseService;

	    @Autowired
	    private UserService userService;

	    @Autowired
	    private OrderService orderService;
    @GetMapping("/home")
    public String staffHome() {
        return "staff/home";
    }

    @GetMapping("/dashboard")
    public String showDashboard(Model model, Authentication authentication) {
    	 logger.info("Bắt đầu xử lý /staff/dashboard");
         if (authentication == null || !authentication.getAuthorities().stream()
                 .anyMatch(auth -> auth.getAuthority().equals("ROLE_Staff"))) {
             logger.warn("Không có quyền truy cập dashboard: {}", authentication != null ? authentication.getName() : "Chưa đăng nhập");
             return "redirect:/login";
         }

         logger.info("Hiển thị dashboard cho staff: {}", authentication.getName());

         try {
             model.addAttribute("totalCourses", courseService.countAllCourses());
             model.addAttribute("totalStudents", userService.countAllStudents());
             model.addAttribute("activeCourses", courseService.countActiveCourses());
             model.addAttribute("pendingOrders", orderService.countPendingOrders());
             model.addAttribute("totalReviews", 0); 
             
             // Top khóa học bán chạy
             List<Object[]> topCourses = courseService.findTopCoursesByEnrollments(5);
             model.addAttribute("topCourses", topCourses);
             logger.info("Top khóa học: {}", topCourses.size());
             
             // Đơn hàng gần đây
             List<Object[]> recentOrders = orderService.findRecentOrders(5);
             model.addAttribute("recentOrders", recentOrders);
             logger.info("Đơn hàng gần đây: {}", recentOrders.size());
             
         } catch (Exception e) {
             logger.error("Lỗi khi lấy dữ liệu thống kê: {}", e.getMessage());
             model.addAttribute("error", "Không thể tải dữ liệu thống kê. Vui lòng thử lại sau.");
         }

         return "staff/dashboard";
    }
    
    @GetMapping("/ordersList")
    public String showOrdersList(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", defaultValue = "all") String status,
            Model model, Authentication authentication) {
        logger.info("Bắt đầu xử lý /staff/ordersList: page={}, size={}, search={}, status={}", page, size, search, status);
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_Staff"))) {
            logger.warn("Không có quyền truy cập ordersList: {}", authentication != null ? authentication.getName() : "Chưa đăng nhập");
            return "redirect:/login";
        }

        try {
            List<Object[]> orders = orderService.findAllOrders(page, size, search, status);
            long totalOrders = orderService.countAllOrders(search, status);
            int totalPages = (int) Math.ceil((double) totalOrders / size);

            model.addAttribute("orders", orders);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalOrders", totalOrders);
            model.addAttribute("search", search);
            model.addAttribute("status", status);
            logger.info("Danh sách đơn hàng: {}, tổng số: {}", orders.size(), totalOrders);
        } catch (Exception e) {
            logger.error("Lỗi khi lấy danh sách đơn hàng: {}", e.getMessage());
            model.addAttribute("error", "Không thể tải danh sách đơn hàng. Vui lòng thử lại sau.");
        }

        return "staff/ordersList";
    }

    @GetMapping("/orderDetail")
    public String showOrderDetail(@RequestParam("orderId") int orderId, Model model, Authentication authentication) {
        logger.info("Bắt đầu xử lý /staff/orderDetail với OrderID: {}", orderId);
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_Staff"))) {
            logger.warn("Không có quyền truy cập orderDetail: {}", authentication != null ? authentication.getName() : "Chưa đăng nhập");
            return "redirect:/login";
        }

        try {
            Object[] orderDetails = orderService.findOrderDetailsById(orderId);
            model.addAttribute("orderDetails", orderDetails);
            model.addAttribute("orderDetailItems", orderDetails[9]); // Danh sách khóa học
            logger.info("Chi tiết đơn hàng {}: {}", orderId, orderDetails);
        } catch (Exception e) {
            logger.error("Lỗi khi lấy chi tiết đơn hàng {}: {}", orderId, e.getMessage());
            model.addAttribute("error", "Không thể tải chi tiết đơn hàng. Vui lòng thử lại sau.");
        }

        return "staff/orderDetail";
    }
    
    @PostMapping("/orderDetail/updateStatus")
    public String updateOrderStatus(
            @RequestParam("orderId") int orderId,
            @RequestParam("orderStatus") String status,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {
        logger.info("Bắt đầu xử lý cập nhật trạng thái đơn hàng {} thành {}", orderId, status);
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_Staff"))) {
            logger.warn("Không có quyền cập nhật trạng thái: {}", authentication != null ? authentication.getName() : "Chưa đăng nhập");
            return "redirect:/login";
        }

        try {
            orderService.updateOrderStatus(orderId, status);
            redirectAttributes.addFlashAttribute("success", "Cập nhật trạng thái đơn hàng thành công!");
        } catch (Exception e) {
            logger.error("Lỗi khi cập nhật trạng thái đơn hàng {}: {}", orderId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Không thể cập nhật trạng thái đơn hàng. Vui lòng thử lại sau.");
        }

        return "redirect:/staff/orderDetail?orderId=" + orderId;
    }
    

    @GetMapping("/categories")
    public String courses(Model model) {
        // TODO: Add course management logic
        return "staff/categories";
    }
} 