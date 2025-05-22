package com.example.demo.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.controller.CheckoutController;
import com.example.demo.model.cart;
import com.example.demo.model.course;
import com.example.demo.model.order;
import com.example.demo.model.orderDetail;
import com.example.demo.model.payment;
import com.example.demo.model.paymentMethod;
import com.example.demo.model.promotion;
import com.example.demo.model.users;
import com.example.demo.repository.CartRepository;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.PromotionRepository;
import com.example.demo.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Service
public class CheckoutService {
	private static final Logger logger = LoggerFactory.getLogger(CheckoutController.class);
	
	@Autowired
    private CartRepository cartRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private VNPAYService vnpayService;
    
    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private EmailService emailService;
    
    public order prepareCheckout(int userId, HttpSession session) {
        List<cart> cartItems = cartRepository.findByUserId(userId);
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Giỏ hàng trống!");
        }

        order order = new order();
        order.setUserID(userId);
        order.setOrderDate(LocalDateTime.now());
        order.setOrderStatus("PENDING");

        session.setAttribute("checkoutOrder", order);
        return order;
    }

    public String applyCoupon(String couponCode, int courseId, int userId, order order) {
    	
    	logger.info("Áp dụng mã giảm giá {} cho courseId {} bởi userId: {}", couponCode, courseId, userId);

        String validationError = promotionRepository.validateCoupon(couponCode, courseId, userId);
        if (validationError != null) {
            logger.warn("Lỗi khi áp dụng mã giảm giá: {}", validationError);
            return "error:" + validationError;
        }

        promotion promotion = promotionRepository.findByCodeAndCourseId(couponCode, courseId);
        if (promotion == null) {
            logger.warn("Mã giảm giá {} không hợp lệ hoặc đã hết hạn cho courseId {}", couponCode, courseId);
            return "error:Mã giảm giá không hợp lệ hoặc đã hết hạn.";
        }
        
        orderDetail targetDetail = order.getOrderDetails().stream()
                .filter(detail -> detail.getCourseID() == courseId)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học trong đơn hàng!"));

        
        targetDetail.setPromotionID(promotion.getPromotionID());

       
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (orderDetail detail : order.getOrderDetails()) {
            BigDecimal price = detail.getPrice();
            if (detail.getPromotionID() != null) {
                promotion appliedPromo = promotionRepository.findById(detail.getPromotionID());
                if (appliedPromo != null) {
                    double discountPercentage = appliedPromo.getDiscountPercentage(); 
                    BigDecimal discountPercentageBD = BigDecimal.valueOf(discountPercentage); 
                    BigDecimal discount = price.multiply(discountPercentageBD).divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
                    price = price.subtract(discount);
                }
            }
            totalAmount = totalAmount.add(price);
        }
        order.setTotalAmount(totalAmount);

        logger.info("Áp dụng mã giảm giá thành công, newTotal: {}", totalAmount);
        return "success:" + totalAmount;
    }

    @Transactional
    public order completeCheckout(order order, users user, HttpServletRequest request, HttpSession session) {
    	
    	logger.info("Bắt đầu hoàn tất thanh toán cho userId: {}", user.getUserID());
        try {
            userRepository.updateUserProfile(user.getUserID(), user.getFullname(), user.getEmail(), user.getPhoneNumber());
            logger.info("Cập nhật hồ sơ người dùng thành công cho userId: {}", user.getUserID());

            List<orderDetail> details = order.getOrderDetails();
            if (details == null || details.isEmpty()) {
                logger.error("Danh sách orderDetails rỗng hoặc null");
                throw new RuntimeException("Không có chi tiết đơn hàng để lưu!");
            }
            for (orderDetail detail : details) {
                if (detail.getCourseID() == 0 || detail.getPrice() == null) {
                    logger.error("orderDetail không hợp lệ: CourseID = {}, Price = {}", detail.getCourseID(), detail.getPrice());
                    throw new RuntimeException("Chi tiết đơn hàng không hợp lệ!");
                }
  
                course course = courseRepository.findById(detail.getCourseID());
                if (course == null) {
                    throw new RuntimeException("Khóa học không tồn tại: " + detail.getCourseID());
                }
                if (course.getQuantity() <= 0) {
                    logger.error("Khóa học {} đã hết hàng", detail.getCourseID());
                    throw new RuntimeException("Khóa học " + course.getTitle() + " đã hết hàng!");
                }
            }

            // Lưu đơn hàng
            order.setOrderStatus("PENDING");
            order = orderRepository.save(order);
         

            if (order.getPromotionID() != null) {
                promotionRepository.saveUserPromotion(order.getPromotionID(), user.getUserID());
                logger.info("Lưu userPromotion cho PromotionID: {} và UserID: {}", order.getPromotionID(), user.getUserID());
            }

            for (orderDetail detail : details) {
                if (detail.getPromotionID() != null) {
                    promotionRepository.saveUserPromotion(detail.getPromotionID(), user.getUserID());
                    logger.info("Lưu userPromotion cho PromotionID: {} và UserID: {}", detail.getPromotionID(), user.getUserID());
                }
                courseRepository.decrementQuantity(detail.getCourseID());
                logger.info("Giảm số lượng khóa học CourseID: {}", detail.getCourseID());
            }

            payment payment = new payment();
            payment.setOrderID(order.getOrderID());
            payment.setCreateDate(LocalDateTime.now());
            payment.setPaymentStatus("Completed");
            payment.setCurrency("VND");
            payment.setAmount(order.getTotalAmount());
            paymentMethod paymentMethod = new paymentMethod();
            paymentMethod.setMethodType("EWallet");
            paymentMethod.seteWalletProvider("VNPAY");
            paymentMethod.seteWalletTransactionID(System.currentTimeMillis());
            payment.setPaymentMethod(paymentMethod);
            payment = paymentRepository.save(payment);
            logger.info("Đã lưu payment với PaymentID: {}", payment.getPaymentID());
            
            try {
                emailService.sendOrderConfirmationEmail(user, order);
            } catch (Exception e) {
                logger.error("Gửi email xác nhận thất bại cho userId {}: {}", user.getUserID(), e.getMessage(), e);
            }
            
            return order;
        } catch (Exception e) {
            logger.error("Lỗi khi lưu đơn hàng: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi khi lưu đơn hàng: " + e.getMessage());
        }
    }

    public void confirmPayment(int orderId, int userId) {
    	payment payment = paymentRepository.findByOrderId(orderId);
        if (payment == null) {
            throw new RuntimeException("Không tìm thấy thông tin thanh toán!");
        }

        try {
            order order = orderRepository.findById(orderId);
            if (userId != -1 && order.getUserID() != userId) {
                throw new RuntimeException("Bạn không có quyền xác nhận đơn hàng này!");
            }
            paymentRepository.updatePaymentStatus(payment.getPaymentID(), "Completed");
        } catch (EmptyResultDataAccessException e) {
            throw new RuntimeException("Không tìm thấy đơn hàng!");
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-FORWARDED-FOR");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
    
    public String createPaymentUrl(order order, users user, HttpServletRequest request) {
        try {
            BigDecimal amount = order.getTotalAmount();
            int orderId = new Random().nextInt(1000000); 
            String ipAddr = request.getRemoteAddr();
            String paymentUrl = vnpayService.createPaymentUrl(request, orderId, amount, ipAddr);
            // Lưu orderId tạm thời vào session
            request.getSession().setAttribute("tempOrderId", orderId);
            return paymentUrl;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo URL thanh toán: " + e.getMessage());
        }
    }
    

}
