package com.example.demo.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.model.cart;
import com.example.demo.model.course;
import com.example.demo.model.order;
import com.example.demo.model.orderDetail;
import com.example.demo.model.payment;
import com.example.demo.model.paymentMethod;
import com.example.demo.model.users;
import com.example.demo.repository.CartRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.CartService;
import com.example.demo.service.CheckoutService;
import com.example.demo.service.CourseService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;


@Controller
public class CheckoutController {
	
	private static final Logger logger = LoggerFactory.getLogger(CheckoutController.class);
	@Autowired
    private CheckoutService checkoutService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository;
    
    @Autowired
    private CartService cartService;
    
    @Autowired
    private CourseService courseService;
    
    
	@GetMapping("/checkout")
	public String showCheckoutPage(Model model, HttpSession session, Authentication authentication,@RequestParam(required = false) String source) {
		
		if (authentication == null) {
            logger.info("Chưa đăng nhập, chuyển hướng đến /login");
            return "redirect:/login";
        }
        String email = authentication.getName();
        logger.info("Xử lý checkout cho email: {}", email);
        users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email: " + email));
        try {
            order order;
            if ("cart".equals(source)) {
                session.removeAttribute("checkoutOrder");
                List<cart> cartItems = cartRepository.findByUserId(user.getUserID());

                if (cartItems.isEmpty()) {
                    logger.warn("Giỏ hàng trống cho userId: {}", user.getUserID());
                    session.setAttribute("error", "Giỏ hàng của bạn trống!");
                    return "redirect:/cart";
                }

                List<cart> enrichedCartItems = cartItems.stream().map(cart -> {
                    course course = courseService.getCourseById(cart.getCourseID());
                    if (course != null) {
                        cart.setCourse(course);
                    } else {
                        logger.warn("Course với ID {} không tồn tại cho cartId {}", cart.getCourseID(), cart.getCartID());
                    }
                    return cart;
                }).collect(Collectors.toList());

                if (enrichedCartItems.stream().anyMatch(cart -> cart.getCourse() == null)) {
                    
                    throw new RuntimeException("Dữ liệu khóa học không hợp lệ!");
                }

                order = new order();
                order.setUserID(user.getUserID());
                order.setOrderDate(java.time.LocalDateTime.now());
                order.setOrderStatus("PENDING");

                BigDecimal totalAmount = BigDecimal.ZERO;
                
                List<orderDetail> orderDetails = new ArrayList<>();
                for (cart cart : enrichedCartItems) {
                    orderDetail detail = new orderDetail();
                    detail.setCourseID(cart.getCourseID());
                    detail.setPrice(cart.getCourse().getPrices());
                    detail.setCourse(cart.getCourse());
                    orderDetails.add(detail);
                    totalAmount = totalAmount.add(cart.getCourse().getPrices().multiply(BigDecimal.valueOf(cart.getQuantity())));
                }
                
                order.setOrderDetails(orderDetails);
                order.setTotalAmount(totalAmount);
                
                session.setAttribute("checkoutOrder", order);
            } else {
                order = (order) session.getAttribute("checkoutOrder");
                if (order != null && "TEMP".equals(order.getOrderStatus())) {
             
                    List<orderDetail> orderDetails = order.getOrderDetails();
                    if (orderDetails == null || orderDetails.isEmpty()) {
                        
                        session.setAttribute("error", "Đơn hàng của bạn trống!");
                        return "redirect:/category";
                    }

                    for (orderDetail detail : orderDetails) {
                        course course = courseService.getCourseById(detail.getCourseID());
                        if (course != null) {
                            detail.setCourse(course);
                        } else {
                            throw new RuntimeException("Khóa học không hợp lệ!");
                        }
                    }
                } else {
                   
                    logger.warn("Phiên thanh toán không hợp lệ cho userId: {}", user.getUserID());
                    session.setAttribute("error", "Phiên thanh toán không hợp lệ!");
                    return "redirect:/cart";
                }
            }

            payment payment = new payment();
            paymentMethod paymentMethod = new paymentMethod();
            paymentMethod.setMethodType("EWallet");
            paymentMethod.seteWalletProvider("VNPAY");
            payment.setPaymentMethod(paymentMethod);
            payment.setAmount(order.getTotalAmount());

            model.addAttribute("order", order);
            model.addAttribute("user", user);
            model.addAttribute("payment", payment);
            model.addAttribute("notificationMessage", session.getAttribute("notificationMessage"));
            model.addAttribute("error", session.getAttribute("error"));
            if (session.getAttribute("error") != null) {
                session.removeAttribute("error");
            }

            String paymentUrl = (String) session.getAttribute("paymentUrl");
            logger.info("Payment URL trong showCheckoutPage: {}", paymentUrl);
            logger.info("Trả về trang checkout");
            return "commons/checkout";
        } catch (Exception e) {
            logger.error("Lỗi khi xử lý checkout: {}", e.getMessage(), e);
            session.setAttribute("error", e.getMessage());
            return "redirect:/cart";
        }
    }
	@PostMapping("/course/buy-now")
	public String buyNow(@RequestParam("courseId") int courseId, HttpSession session, Authentication authentication) {
		if(authentication == null) {
			return "redirect:/login";
		}
		String email = authentication.getName();
		users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email: " + email));
		
		try {
			course course = courseService.getCourseById(courseId);
			if(course == null) {
				
                session.setAttribute("error", "Khóa học không tồn tại!");
                return "redirect:/category";
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
            logger.info("Đã tạo order tạm thời cho userId: {}, courseId: {}", user.getUserID(), courseId);
            return "redirect:/checkout";
			
		}catch (Exception e) {
			logger.error("Lỗi khi tiến hành thanh toán: {}", e.getMessage(), e);
            session.setAttribute("error", "Lỗi khi tiến hành thanh toán: " + e.getMessage());
            return "redirect:/checkout";
			
		}
	}
	
	
	@PostMapping("/checkout/apply-coupon")
    @ResponseBody
    public String applyCoupon(@RequestParam String couponCode, @RequestParam int courseId, HttpSession session, Authentication authentication) {
		if (authentication == null) {
            return "error:Vui lòng đăng nhập!";
        }

        String email = authentication.getName();
        users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));
        order order = (order) session.getAttribute("checkoutOrder");

        if (order == null) {
            return "error:Phiên thanh toán không hợp lệ!";
        }

        String result = checkoutService.applyCoupon(couponCode, courseId, user.getUserID(), order);
        if (result.startsWith("success")) {
            session.setAttribute("checkoutOrder", order);
        }
        return result;
    }

    @PostMapping("/checkout/complete")
    public String completeCheckout(@ModelAttribute users user, HttpServletRequest request, HttpSession session, Model model, Authentication authentication) {
    	if (authentication == null) {
            logger.info("Chưa đăng nhập, chuyển hướng đến /login");
            return "redirect:/login";
        }
        String email = authentication.getName();
        logger.info("Tiến hành thanh toán cho email: {}", email);
        users currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email: " + email));
        order order = (order) session.getAttribute("checkoutOrder");
        if (order == null) {
        	
            logger.warn("Phiên thanh toán không hợp lệ cho email: {}", email);
            session.setAttribute("error", "Phiên thanh toán không hợp lệ!");
            return "redirect:/cart";
        }
        try {
        	
            user.setUserID(currentUser.getUserID());
            session.setAttribute("checkoutUser", user);
            session.setAttribute("source", request.getParameter("source")); 
            order.setOrderStatus("TEMP");

            String paymentUrl = checkoutService.createPaymentUrl(order, user, request);
            if (paymentUrl == null) {
            	
                throw new RuntimeException("Không thể tạo URL thanh toán!");
            }
            session.setAttribute("paymentUrl", paymentUrl);
            
            return "redirect:" + paymentUrl;
        } catch (Exception e) {
           
        	
            session.setAttribute("error", "Lỗi khi tiến hành thanh toán: " + e.getMessage());
            return "redirect:/checkout";
        }
    }

    @GetMapping("/checkout/vnpay-return")
    public String handleVNPAYReturn(@RequestParam Map<String, String> params, HttpSession session) {
    	String vnp_TxnRef = params.get("vnp_TxnRef");
        String vnp_ResponseCode = params.get("vnp_ResponseCode");
        logger.info("Nhận callback VNPAY: vnp_TxnRef={}, vnp_ResponseCode={}", vnp_TxnRef, vnp_ResponseCode);
        try {
            if ("00".equals(vnp_ResponseCode)) {
                order order = (order) session.getAttribute("checkoutOrder");
                users user = (users) session.getAttribute("checkoutUser");
                if (order == null || user == null) {
                    logger.warn("Không tìm thấy thông tin đơn hàng hoặc người dùng trong session");
                    session.setAttribute("error", "Phiên thanh toán không hợp lệ!");
                    return "redirect:/category?error=true";
                }

                order = checkoutService.completeCheckout(order, user, null, session);
                checkoutService.confirmPayment(order.getOrderID(), -1);
                session.setAttribute("notificationMessage", "Thanh toán thành công! Thông tin khóa học đã được gửi đến email của bạn.");
                logger.info("Thanh toán thành công cho đơn hàng: {}", order.getOrderID());

                // Xóa session
                session.removeAttribute("checkoutOrder");
                session.removeAttribute("checkoutUser");
                session.removeAttribute("paymentUrl");
                session.removeAttribute("tempOrderId");
                session.removeAttribute("source");
                return "redirect:/category?success=true";
            } else {
                logger.warn("Thanh toán thất bại hoặc bị hủy: Mã lỗi {}", vnp_ResponseCode);
                session.setAttribute("error", "Thanh toán thất bại hoặc bị hủy: Mã lỗi " + vnp_ResponseCode);

                session.removeAttribute("checkoutOrder");
                session.removeAttribute("checkoutUser");
                session.removeAttribute("paymentUrl");
                session.removeAttribute("tempOrderId");
                session.removeAttribute("source");
                return "redirect:/category?error=true";
            }
        } catch (Exception e) {
            logger.error("Lỗi xử lý callback VNPAY: {}", e.getMessage(), e);
            session.setAttribute("error", "Lỗi xử lý callback VNPAY: " + e.getMessage());
            
            session.removeAttribute("checkoutOrder");
            session.removeAttribute("checkoutUser");
            session.removeAttribute("paymentUrl");
            session.removeAttribute("tempOrderId");
            session.removeAttribute("source");
            return "redirect:/category?error=true";
        }
    }

    @PostMapping("/checkout/confirm-payment")
    @ResponseBody
    public String confirmPayment(@RequestParam int orderId, Authentication authentication) {
    	if (authentication == null) {
            return "error:Vui lòng đăng nhập!";
        }

        String email = authentication.getName();
        users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        try {
            checkoutService.confirmPayment(orderId, user.getUserID());
            return "success";
        } catch (Exception e) {
            return "error:Lỗi xác nhận thanh toán: " + e.getMessage();
        }
    }
    
    
	
    @PostMapping("/checkout/clear-payment-url")
    @ResponseBody
    public void clearPaymentUrl(HttpSession session) {
        session.removeAttribute("paymentUrl");
    }

    @PostMapping("/checkout/clear-session")
    @ResponseBody
    public void clearSession(HttpSession session) {
        session.removeAttribute("notificationMessage");
        session.removeAttribute("error");
        session.removeAttribute("paymentUrl");
    }

}
