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
	public String showCheckoutPage(Model model, HttpSession session, Authentication authentication) {
		
		if (authentication == null) {
            logger.info("Chưa đăng nhập, chuyển hướng đến /login");
            return "redirect:/login";
        }
        String email = authentication.getName();
        logger.info("Xử lý checkout cho email: {}", email);
        users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email: " + email));
        try {
            logger.info("Chuẩn bị checkout cho userId: {}", user.getUserID());
            order order = checkoutService.prepareCheckout(user.getUserID(), session);
            List<cart> cartItems = cartRepository.findByUserId(user.getUserID());
            logger.info("Số lượng cartItems: {}", cartItems.size());
            if (cartItems.isEmpty()) {
                logger.warn("Giỏ hàng trống cho userId: {}", user.getUserID());
                session.setAttribute("error", "Giỏ hàng của bạn trống!");
                return "redirect:/cart";
            }

            // Làm giàu cartItems với thông tin course
            List<cart> enrichedCartItems = cartItems.stream().map(cart -> {
                course course = courseService.getCourseById(cart.getCourseID());
                if (course != null) {
                    cart.setCourse(course);
                } else {
                    logger.warn("Course với ID {} không tồn tại cho cartId {}", cart.getCourseID(), cart.getCartID());
                }
                return cart;
            }).collect(Collectors.toList());

            // Kiểm tra nếu có cartItem thiếu course
            if (enrichedCartItems.stream().anyMatch(cart -> cart.getCourse() == null)) {
                logger.error("Một hoặc nhiều khóa học không hợp lệ trong giỏ hàng của userId: {}", user.getUserID());
                throw new RuntimeException("Dữ liệu khóa học không hợp lệ!");
            }

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

            payment payment = new payment();
            paymentMethod paymentMethod = new paymentMethod();
            paymentMethod.setMethodType("EWallet");
            paymentMethod.seteWalletProvider("VNPAY");
            payment.setPaymentMethod(paymentMethod);
            payment.setAmount(order.getTotalAmount());

            model.addAttribute("order", order);
            model.addAttribute("user", user);
            model.addAttribute("payment", payment);
            model.addAttribute("notificationMessage", "Thanh toán thành công! Thông tin khóa học đã được gửi đến email của bạn.");
            if (session.getAttribute("error") != null) {
                model.addAttribute("error", session.getAttribute("error"));
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
            String paymentUrl = checkoutService.createPaymentUrl(order, user, request);
            if (paymentUrl == null) {
                logger.error("Không thể tạo URL thanh toán!");
                throw new RuntimeException("Không thể tạo URL thanh toán!");
            }
            session.setAttribute("paymentUrl", paymentUrl);
            logger.info("Chuyển hướng đến trang VNPAY: {}", paymentUrl);
            return "redirect:" + paymentUrl;
        } catch (Exception e) {
            logger.error("Lỗi khi tiến hành thanh toán: {}", e.getMessage(), e);
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
                    return "redirect:/checkout?error=true";
                }
                order = checkoutService.completeCheckout(order, user, null, session);
                checkoutService.confirmPayment(order.getOrderID(), -1);
                session.setAttribute("notificationMessage", "Thanh toán thành công! Thông tin khóa học đã được gửi đến email của bạn.");
                logger.info("Thanh toán thành công cho đơn hàng: {}", order.getOrderID());
                session.removeAttribute("checkoutOrder");
                session.removeAttribute("checkoutUser");
                session.removeAttribute("paymentUrl");
                return "redirect:/checkout?success=true";
            } else {
                logger.warn("Thanh toán thất bại: Mã lỗi {}", vnp_ResponseCode);
                session.setAttribute("error", "Thanh toán thất bại: Mã lỗi " + vnp_ResponseCode);
                return "redirect:/checkout?error=true";
            }
        } catch (Exception e) {
            logger.error("Lỗi xử lý callback VNPAY: {}", e.getMessage(), e);
            session.setAttribute("error", "Lỗi xử lý callback VNPAY: " + e.getMessage());
            return "redirect:/checkout?error=true";
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
	
	

}
