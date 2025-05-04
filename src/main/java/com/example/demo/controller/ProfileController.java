package com.example.demo.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.model.course;
import com.example.demo.model.order;
import com.example.demo.model.orderDetail;
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

        // Lấy tất cả đơn hàng của người dùng
        List<order> orders = orderRepository.findByUserId(user.getUserID());

        model.addAttribute("user", user);
        model.addAttribute("orders", orders);

        logger.info("Tìm thấy {} đơn hàng cho userId: {}", orders.size(), user.getUserID());
        return "commons/profile";
	}
	
	@PostMapping("/profile/update")
    public String updateProfile(
            @RequestParam("userID") int userID,
            @RequestParam("fullName") String fullName,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "address", required = false) String address,
            @RequestParam(value = "gender", required = false) String gender,
            Authentication authentication) {
		if (authentication == null) {
            logger.info("Chưa đăng nhập, không thể cập nhật thông tin");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập để cập nhật thông tin");
        }

        String email = authentication.getName();
        users currentUser = userService.findByEmailOrPhone(email);
        if (currentUser == null || currentUser.getUserID() != userID) {
            logger.warn("Không tìm thấy người dùng hoặc userID không khớp: userID={}", userID);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền cập nhật thông tin này");
        }

        // Kiểm tra dữ liệu đầu vào
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Họ và tên không được để trống");
        }

        if (phone != null && !phone.trim().isEmpty() && !phone.matches("^0[0-9]{9}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại phải bắt đầu bằng 0 và có 10 chữ số");
        }

        if (!gender.matches("^(MALE|FEMALE|OTHER)$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giới tính phải là MALE, FEMALE hoặc OTHER");
        }

        // Cập nhật thông tin người dùng
        users user = new users();
        user.setUserID(userID);
        user.setFullname(fullName.trim());
        user.setPhoneNumber(phone != null ? phone.trim() : null);
        user.setAddress(address != null ? address.trim() : null);
        user.setGender(gender);

        try {
            userService.updateUser(user);
            return "redirect:/profile";
        } catch (Exception e) {
            logger.error("Lỗi khi cập nhật thông tin người dùng: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
	}
        
        @PostMapping("/profile/change-password")
        public ResponseEntity<String> changePassword(
                @RequestParam("currentPassword") String currentPassword,
                @RequestParam("newPassword") String newPassword,
                @RequestParam("confirmNewPassword") String confirmNewPassword,
                Authentication authentication) {
            try {
                if (authentication == null) {
                    logger.info("Chưa đăng nhập, không thể đổi mật khẩu");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("error:Vui lòng đăng nhập để đổi mật khẩu!");
                }

                String email = authentication.getName();
                users user = userService.findByEmailOrPhone(email);
                if (user == null) {
                    logger.warn("Không tìm thấy người dùng với email: {}", email);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("error:Không tìm thấy người dùng!");
                }

                // Kiểm tra mật khẩu hiện tại
                if (!userService.checkPassword(user, currentPassword)) {
                    logger.warn("Mật khẩu hiện tại không đúng cho email: {}", email);
                    return ResponseEntity.badRequest().body("error:Mật khẩu hiện tại không đúng!");
                }

                // Kiểm tra mật khẩu mới và xác nhận
                if (!newPassword.equals(confirmNewPassword)) {
                    logger.warn("Mật khẩu mới và xác nhận không khớp cho email: {}", email);
                    return ResponseEntity.badRequest().body("error:Mật khẩu mới và xác nhận không khớp!");
                }

                // Kiểm tra độ dài mật khẩu mới
                if (newPassword.length() < 6) {
                    logger.warn("Mật khẩu mới quá ngắn cho email: {}", email);
                    return ResponseEntity.badRequest().body("error:Mật khẩu mới phải có ít nhất 6 ký tự!");
                }

                // Cập nhật mật khẩu
                userService.updatePassword(user, newPassword);
                logger.info("Đổi mật khẩu thành công cho email: {}", email);
                return ResponseEntity.ok("success:Mật khẩu đã được thay đổi thành công!");
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("error:" + e.getMessage());
            }
    }
}
