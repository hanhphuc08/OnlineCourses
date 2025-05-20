
package com.example.demo.controller.owner;


import com.example.demo.model.category;
import com.example.demo.model.course;
import com.example.demo.model.courseStatus;
import com.example.demo.model.promotion;
import com.example.demo.model.users;

import com.example.demo.model.*;

import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.CourseRepository;
import com.example.demo.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/owner")
@PreAuthorize("hasRole('Owner')")
public class OwnerController {

    private static final Logger logger = LoggerFactory.getLogger(OwnerController.class);

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserService userService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private CourseService courseService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private PromotionService promotionService;
    @Autowired
    private OrderService orderService;

    @GetMapping("/home")
    public String ownerHome() {
        return "owner/home";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
    	logger.info("Bắt đầu xử lý /owner/dashboard");
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_Owner"))) {
            logger.warn("Không có quyền truy cập dashboard: {}", authentication != null ? authentication.getName() : "Chưa đăng nhập");
            return "redirect:/login";
        }
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
        return "owner/dashboard";
    }

    @GetMapping("/users")
    public String userManagement(Model model, Authentication authentication) {
    	logger.info("Bắt đầu xử lý /owner/users");
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_Owner"))) {
            logger.warn("Không có quyền truy cập users: {}", authentication != null ? authentication.getName() : "Chưa đăng nhập");
            return "redirect:/login";
        }
        return "owner/users";
    }

    @GetMapping("/productsList")
    public String productsList(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", defaultValue = "all") String status,
            Model model, Authentication authentication) {
        logger.info("Bắt đầu xử lý /owner/productsList: page={}, size={}, search={}, status={}", page, size, search, status);
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_Owner"))) {
            logger.warn("Không có quyền truy cập productsList: {}", authentication != null ? authentication.getName() : "Chưa đăng nhập");
            return "redirect:/login";
        }

        try {
            List<course> courseList = courseService.getCoursesPaginated(page, size, search, status);
            List<category> categories = categoryService.getAllCategories();
            long totalCourses = courseService.countCourses(search, status);
            int totalPages = (int) Math.ceil((double) totalCourses / size);

            model.addAttribute("courses", courseList);
            model.addAttribute("categories", categories);
            model.addAttribute("hasCourses", !courseList.isEmpty());
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalCourses", totalCourses);
            model.addAttribute("search", search);
            model.addAttribute("status", status);
            logger.info("Danh sách khóa học: {}, danh mục: {}, tổng số trang: {}", courseList.size(), categories.size(), totalPages);
        } catch (Exception e) {
            logger.error("Lỗi khi lấy danh sách khóa học hoặc danh mục: {}", e.getMessage());
            model.addAttribute("error", "Không thể tải danh sách khóa học hoặc danh mục. Vui lòng thử lại sau.");
        }
        return "owner/productsList";
    }

    @GetMapping("/ordersList")
    public String showOrdersList(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", defaultValue = "all") String status,
            Model model, Authentication authentication) {
        logger.info("Bắt đầu xử lý /owner/ordersList: page={}, size={}, search={}, status={}", page, size, search, status);
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_Owner"))) {
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
        return "owner/ordersList";
    }

    @GetMapping("/orderDetail")
    public String showOrderDetail(@RequestParam("orderId") int orderId, Model model, Authentication authentication) {
        logger.info("Bắt đầu xử lý /owner/orderDetail với OrderID: {}", orderId);
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_Owner"))) {
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

        return "owner/orderDetail";
    }

    @PostMapping("/orderDetail/updateStatus")
    public String updateOrderStatus(
            @RequestParam("orderId") int orderId,
            @RequestParam("orderStatus") String status,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {
        logger.info("Bắt đầu xử lý cập nhật trạng thái đơn hàng {} thành {}", orderId, status);
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_Owner"))) {
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

        return "redirect:/owner/orderDetail?orderId=" + orderId;
    }

    @GetMapping("/addProducts")
    public String addCourseForm(Model model, Authentication authentication) {
        logger.info("Bắt đầu xử lý /owner/addProducts (form thêm khóa học)");
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_Owner"))) {
            logger.warn("Không có quyền truy cập addProducts: {}", authentication != null ? authentication.getName() : "Chưa đăng nhập");
            return "redirect:/login";
        }

        try {
            model.addAttribute("course", new course());
            model.addAttribute("categories", categoryService.getAllCategories());
            logger.info("Hiển thị form thêm khóa học");
        } catch (Exception e) {
            logger.error("Lỗi khi tải form thêm khóa học: {}", e.getMessage());
            model.addAttribute("error", "Không thể tải form thêm khóa học. Vui lòng thử lại sau.");
        }
        return "owner/addProducts";
    }


    @GetMapping("/customer")
    public String getCustomers(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) Integer status,
            Model model, Authentication authentication) {
        logger.info("Bắt đầu xử lý /owner/customer: page={}, size={}, search={}, status={}", page, size, search, status);
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_Owner"))) {
            logger.warn("Không có quyền truy cập customer: {}", authentication != null ? authentication.getName() : "Chưa đăng nhập");
            return "redirect:/login";
        }

        try {
            List<users> customers = userService.getCustomersPaginated(page, size, search, status);
            long totalCustomers = userService.countCustomers(search, status);
            int totalPages = (int) Math.ceil((double) totalCustomers / size);

            model.addAttribute("customers", customers);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalCustomers", totalCustomers);
            model.addAttribute("search", search);
            model.addAttribute("status", status);
            logger.info("Danh sách khách hàng: {}, tổng số: {}", customers.size(), totalCustomers);
        } catch (Exception e) {
            logger.error("Lỗi khi lấy danh sách khách hàng: {}", e.getMessage());
            model.addAttribute("error", "Không thể tải danh sách khách hàng. Vui lòng thử lại sau.");
        }
        return "owner/customer";
    }

    @GetMapping("/customer/{id}")
    public String getCustomerDetail(@PathVariable("id") int userId, Model model, Authentication authentication) {
        logger.info("Bắt đầu xử lý /owner/customer/{} để hiển thị thông tin chi tiết khách hàng", userId);
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_Owner"))) {
            logger.warn("Không có quyền truy cập thông tin khách hàng: {}", authentication != null ? authentication.getName() : "Chưa đăng nhập");
            return "redirect:/login";
        }

        try {
            users customer = userService.findByUid(String.valueOf(userId));
            if (customer == null) {
                logger.warn("Không tìm thấy khách hàng với UserID: {}", userId);
                model.addAttribute("error", "Không tìm thấy khách hàng.");
                return "owner/customerDetail";
            }
            // Lấy danh sách đơn hàng của khách hàng
            List<order> orders = orderService.findByUserId(userId);
            customer.setOrders(orders);

            model.addAttribute("customer", customer);
            logger.info("Tìm thấy khách hàng: UserID={}, Email={}", userId, customer.getEmail());
        } catch (Exception e) {
            logger.error("Lỗi khi lấy thông tin khách hàng UserID {}: {}", userId, e.getMessage());
            model.addAttribute("error", "Không thể tải thông tin khách hàng. Vui lòng thử lại sau.");
        }
        return "owner/customerDetail";
    }

    @PostMapping("/customer/update")
    @PreAuthorize("hasRole('Owner')")
    public ResponseEntity<String> updateCustomer(@ModelAttribute users updatedUser) {
        try {
            logger.info("Received customer update request for userID: {}", updatedUser.getUserID());

            // Validate required fields
            if (updatedUser.getUserID() <= 0) {
                logger.error("Invalid User ID: {}", updatedUser.getUserID());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid User ID");
            }
            if (updatedUser.getFullname() == null || updatedUser.getFullname().trim().isEmpty()) {
                logger.error("Full name is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Full name is required");
            }
            if (updatedUser.getEmail() == null || updatedUser.getEmail().trim().isEmpty()) {
                logger.error("Email is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email is required");
            }
            if (!updatedUser.getEmail().matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
                logger.error("Invalid email format: {}", updatedUser.getEmail());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid email format");
            }
            if (updatedUser.getPhoneNumber() == null || updatedUser.getPhoneNumber().trim().isEmpty()) {
                logger.error("Phone number is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Phone number is required");
            }
            if (!updatedUser.getPhoneNumber().matches("^\\+?\\d{10,15}$")) {
                logger.error("Invalid phone number format: {}", updatedUser.getPhoneNumber());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid phone number format (must be 10-15 digits, optionally starting with +)");
            }

            // Validate Gender
            if (updatedUser.getGender() == null || updatedUser.getGender().trim().isEmpty()) {
                logger.error("Gender is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Gender is required");
            }
            if (!updatedUser.getGender().equals("MALE") && !updatedUser.getGender().equals("FEMALE") && !updatedUser.getGender().equals("OTHER")) {
                logger.error("Invalid Gender: {}. Must be 'MALE', 'FEMALE', or 'OTHER'", updatedUser.getGender());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid Gender. Must be 'MALE', 'FEMALE', or 'OTHER'");
            }

            // Validate Status
            if (updatedUser.getStatus() != 0 && updatedUser.getStatus() != 1) {
                logger.error("Invalid Status: {}. Must be 0 or 1", updatedUser.getStatus());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid Status. Must be 0 or 1");
            }

            // Fetch existing user
            users existingUser = userService.findByUid(String.valueOf(updatedUser.getUserID()));
            if (existingUser == null) {
                logger.error("User not found with ID: {}", updatedUser.getUserID());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }

            
            existingUser.setFullname(updatedUser.getFullname());
            existingUser.setEmail(updatedUser.getEmail());
            existingUser.setPhoneNumber(updatedUser.getPhoneNumber());
            existingUser.setGender(updatedUser.getGender());
            existingUser.setAddress(updatedUser.getAddress());
            existingUser.setStatus(updatedUser.getStatus());

            // Save updated user
            userService.updateUser(existingUser);
            logger.info("Customer updated successfully: userID={}", updatedUser.getUserID());
            return ResponseEntity.ok("Customer updated successfully");
        } catch (Exception e) {
            logger.error("Error updating customer: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating customer: " + e.getMessage());
        }

    }
    

    @GetMapping("/settings")
    public String settings(Model model, Authentication authentication) {
    	logger.info("Bắt đầu xử lý /owner/settings");
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_Owner"))) {
            logger.warn("Không có quyền truy cập settings: {}", authentication != null ? authentication.getName() : "Chưa đăng nhập");
            return "redirect:/login";
        }
        return "owner/settings";
    }

    @GetMapping("/categories")
    public ResponseEntity<List<category>> getAllCategories() {
        try {
            List<category> categories = categoryRepository.findAllCategories();
            logger.info("Fetched {} categories", categories.size());
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            logger.error("Error fetching categories: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/course/edit")
    public ResponseEntity<?> updateCourse(
            @RequestParam("courseID") int courseID,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("prices") BigDecimal prices,
            @RequestParam("status") String status,
            @RequestParam(value = "image", required = false) String image,
            @RequestParam("quantity") int quantity,
            @RequestParam("categoryID") int categoryID,
            Authentication authentication) {
        logger.info("Bắt đầu xử lý /owner/course/edit với CourseID: {}", courseID);
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_Owner"))) {
            logger.warn("Không có quyền chỉnh sửa khóa học: {}", authentication != null ? authentication.getName() : "Chưa đăng nhập");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Không có quyền chỉnh sửa khóa học");
        }

        try {
            if (courseID <= 0) {
                logger.error("ID khóa học không hợp lệ: {}", courseID);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("ID khóa học không hợp lệ");
            }
            if (title == null || title.trim().isEmpty()) {
                logger.error("Tiêu đề là bắt buộc");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Tiêu đề là bắt buộc");
            }
            if (prices == null || prices.doubleValue() < 0) {
                logger.error("Giá phải không âm: {}", prices);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Giá phải không âm");
            }
            if (quantity < 0) {
                logger.error("Số lượng phải không âm: {}", quantity);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Số lượng phải không âm");
            }
            if (status == null || (!status.equals("ACTIVE") && !status.equals("INACTIVE"))) {
                logger.error("Trạng thái không hợp lệ: {}", status);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Trạng thái phải là ACTIVE hoặc INACTIVE");
            }
            if (categoryID <= 0) {
                logger.error("ID danh mục không hợp lệ: {}", categoryID);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("ID danh mục không hợp lệ");
            }

            course existingCourse = courseService.getCourseById(courseID);
            if (existingCourse == null) {
                logger.error("Không tìm thấy khóa học: {}", courseID);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy khóa học");
            }

            course course = new course();
            course.setCourseID(courseID);
            course.setTitle(title);
            course.setDescription(description);
            course.setPrices(prices);
            course.setStatus(courseStatus.valueOf(status));
            course.setImage(image);
            course.setQuantity(quantity);
            course.setDuration(existingCourse.getDuration());
            course.setCreateAt(existingCourse.getCreateAt());

            category cat = new category();
            cat.setCategoryID(categoryID);
            course.setCategory(cat);

            courseService.updateCourse(course);
            logger.info("Cập nhật khóa học thành công: {}", courseID);
            return ResponseEntity.ok("Cập nhật khóa học thành công!");
        } catch (Exception e) {
            logger.error("Lỗi khi cập nhật khóa học: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Không thể cập nhật khóa học: " + e.getMessage());
        }
    }

    @PostMapping("/course/add")
    public ResponseEntity<?> addCourse(
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("prices") BigDecimal prices,
            @RequestParam("status") String status,
            @RequestParam("quantity") int quantity,
            @RequestParam("categoryID") int categoryID,
            @RequestParam(value = "duration", required = false) String duration,
            @RequestParam("imageUrl") String imageUrl,
            Authentication authentication) {
        logger.info("Bắt đầu xử lý /owner/course/add: title={}, categoryID={}", title, categoryID);
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_Owner"))) {
            logger.warn("Không có quyền thêm khóa học: {}", authentication != null ? authentication.getName() : "Chưa đăng nhập");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Không có quyền thêm khóa học");
        }

        try {
            if (title == null || title.trim().isEmpty()) {
                logger.error("Tiêu đề là bắt buộc");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Tiêu đề là bắt buộc");
            }
            if (prices == null || prices.doubleValue() < 0) {
                logger.error("Giá phải không âm: {}", prices);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Giá phải không âm");
            }
            if (quantity < 0) {
                logger.error("Số lượng phải không âm: {}", quantity);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Số lượng phải không âm");
            }
            if (status == null || (!status.equals("ACTIVE") && !status.equals("INACTIVE"))) {
                logger.error("Trạng thái không hợp lệ: {}", status);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Trạng thái phải là ACTIVE hoặc INACTIVE");
            }
            if (categoryID <= 0) {
                logger.error("ID danh mục không hợp lệ: {}", categoryID);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("ID danh mục không hợp lệ");
            }
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                logger.error("URL hình ảnh là bắt buộc");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("URL hình ảnh là bắt buộc");
            }

            course course = new course();
            course.setTitle(title);
            course.setDescription(description);
            course.setPrices(prices);
            course.setStatus(courseStatus.valueOf(status));
            course.setQuantity(quantity);
            course.setCreateAt(LocalDateTime.now());
            course.setImage(imageUrl);

            if (duration != null && !duration.trim().isEmpty()) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    LocalDateTime durationDate = LocalDate.parse(duration, formatter).atStartOfDay();
                    LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
                    if (durationDate.isBefore(today)) {
                        logger.error("Thời lượng không hợp lệ: phải từ ngày hiện tại trở đi, duration={}", duration);
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Thời lượng phải từ ngày hiện tại trở đi!");
                    }
                    course.setDuration(durationDate);
                } catch (Exception e) {
                    logger.error("Định dạng thời lượng không hợp lệ: {}", duration);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Định dạng thời lượng không hợp lệ. Sử dụng yyyy-MM-dd");
                }
            } else {
                course.setDuration(null);
            }

            category cat = new category();
            cat.setCategoryID(categoryID);
            course.setCategory(cat);

            courseService.addCourse(course);
            logger.info("Thêm khóa học thành công: title={}", title);
            return ResponseEntity.ok("Thêm khóa học thành công!");
        } catch (Exception e) {
            logger.error("Lỗi khi thêm khóa học: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Không thể thêm khóa học: " + e.getMessage());
        }
    }


    @GetMapping("/promotionList")
    public String promotionList(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", defaultValue = "all") String status,
            Model model, Authentication authentication) {
        logger.info("Bắt đầu xử lý /owner/promotionList: page={}, size={}, search={}, status={}", page, size, search, status);
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_Owner"))) {
            logger.warn("Không có quyền truy cập promotionList: {}", authentication != null ? authentication.getName() : "Chưa đăng nhập");
            return "redirect:/login";
        }
        try {
            List<promotion> promotionList = promotionService.getPromotionsPaginated(page, size, search, status);
            List<course> courseList = courseService.getAllCourses();
            long totalPromotions = promotionService.countPromotions(search, status);
            int totalPages = (int) Math.ceil((double) totalPromotions / size);

            model.addAttribute("promotions", promotionList);
            model.addAttribute("courses", courseList);
            model.addAttribute("totalPromotions", totalPromotions);
            model.addAttribute("hasPromotions", !promotionList.isEmpty());
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("search", search);
            model.addAttribute("status", status);
            logger.info("Danh sách khuyến mãi: {}, tổng số: {}", promotionList.size(), totalPromotions);
        } catch (Exception e) {
            logger.error("Lỗi khi lấy danh sách khuyến mãi: {}", e.getMessage());
            model.addAttribute("error", "Không thể tải danh sách khuyến mãi. Vui lòng thử lại sau.");
        }
        return "owner/promotionList";
    }
    
    @PostMapping("/promotion/add")
    public ResponseEntity<?> addPromotion(
            @RequestParam("code") String code,
            @RequestParam("discountPercentage") Double discountPercentage,
            @RequestParam("expirationDate") String expirationDate,
            @RequestParam("courseID") int courseID,
            @RequestParam("status") String status,
            @RequestParam("usageLimit") int usageLimit,
            Authentication authentication) {
        logger.info("Bắt đầu xử lý /owner/promotion/add: code={}, courseID={}", code, courseID);
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_Owner"))) {
            logger.warn("Không có quyền thêm khuyến mãi: {}", authentication != null ? authentication.getName() : "Chưa đăng nhập");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Không có quyền thêm khuyến mãi");
        }
        try {
            if (code == null || code.trim().isEmpty()) {
                logger.error("Mã khuyến mãi là bắt buộc");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Mã khuyến mãi là bắt buộc");
            }
            if (discountPercentage < 0 || discountPercentage > 100) {
                logger.error("Phần trăm giảm giá phải từ 0 đến 100: {}", discountPercentage);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Phần trăm giảm giá phải từ 0 đến 100");
            }
            if (expirationDate == null || expirationDate.trim().isEmpty()) {
                logger.error("Ngày hết hạn là bắt buộc");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Ngày hết hạn là bắt buộc");
            }
            if (courseID <= 0) {
                logger.error("ID khóa học không hợp lệ: {}", courseID);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("ID khóa học không hợp lệ");
            }
            if (status == null || (!status.equals("ACTIVE") && !status.equals("EXPIRED") && !status.equals("USED_OUT"))) {
                logger.error("Trạng thái không hợp lệ: {}", status);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Trạng thái phải là ACTIVE, EXPIRED hoặc USED_OUT");
            }
            if (usageLimit <= 0) {
                logger.error("Giới hạn sử dụng phải lớn hơn 0: {}", usageLimit);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Giới hạn sử dụng phải lớn hơn 0");
            }
            promotion existingPromotion = promotionService.findByCode(code);
            if (existingPromotion != null) {
                logger.error("Mã khuyến mãi đã tồn tại: {}", code);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Mã khuyến mãi đã tồn tại");
            }
            promotion promotion = new promotion();
            promotion.setCode(code);
            promotion.setDiscountPercentage(discountPercentage);
            promotion.setExpirationDate(LocalDateTime.parse(expirationDate, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
            promotion.setCourseID(courseID);
            promotion.setStatus(status);
            promotion.setUsageLimit(usageLimit);
            promotion.setUsageCount(0);
            promotion.setCreateAt(LocalDateTime.now());
            promotionService.addPromotion(promotion);
            logger.info("Thêm khuyến mãi thành công: code={}", code);
            return ResponseEntity.ok("Thêm khuyến mãi thành công!");
        } catch (Exception e) {
            logger.error("Lỗi khi thêm khuyến mãi: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Không thể thêm khuyến mãi: " + e.getMessage());
        }
    }

    
    @PostMapping("/promotion/edit")
    public ResponseEntity<?> updatePromotion(
            @RequestParam("promotionID") int promotionID,
            @RequestParam("code") String code,
            @RequestParam("discountPercentage") double discountPercentage,
            @RequestParam("expirationDate") String expirationDate,
            @RequestParam("courseID") int courseID,
            @RequestParam("status") String status,
            @RequestParam("usageLimit") int usageLimit,
            Authentication authentication) {
        logger.info("Bắt đầu xử lý /owner/promotion/edit với PromotionID: {}", promotionID);
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_Owner"))) {
            logger.warn("Không có quyền chỉnh sửa khuyến mãi: {}", authentication != null ? authentication.getName() : "Chưa đăng nhập");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Không có quyền chỉnh sửa khuyến mãi");
        }
        try {
            if (promotionID <= 0) {
                logger.error("ID khuyến mãi không hợp lệ: {}", promotionID);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("ID khuyến mãi không hợp lệ");
            }
            if (code == null || code.trim().isEmpty()) {
                logger.error("Mã khuyến mãi là bắt buộc");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Mã khuyến mãi là bắt buộc");
            }
            if (discountPercentage < 0 || discountPercentage > 100) {
                logger.error("Phần trăm giảm giá phải từ 0 đến 100: {}", discountPercentage);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Phần trăm giảm giá phải từ 0 đến 100");
            }
            if (expirationDate == null || expirationDate.trim().isEmpty()) {
                logger.error("Ngày hết hạn là bắt buộc");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Ngày hết hạn là bắt buộc");
            }
            if (courseID <= 0) {
                logger.error("ID khóa học không hợp lệ: {}", courseID);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("ID khóa học không hợp lệ");
            }
            if (status == null || (!status.equals("ACTIVE") && !status.equals("EXPIRED") && !status.equals("USED_OUT"))) {
                logger.error("Trạng thái không hợp lệ: {}", status);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Trạng thái phải là ACTIVE, EXPIRED hoặc USED_OUT");
            }
            if (usageLimit <= 0) {
                logger.error("Giới hạn sử dụng phải lớn hơn 0: {}", usageLimit);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Giới hạn sử dụng phải lớn hơn 0");
            }
            promotion existingPromotion = promotionService.getPromotionById(promotionID);
            if (existingPromotion == null) {
                logger.error("Không tìm thấy khuyến mãi: {}", promotionID);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy khuyến mãi");
            }
            if (!existingPromotion.getCode().equals(code) && promotionService.findByCode(code) != null) {
                logger.error("Mã khuyến mãi đã tồn tại: {}", code);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Mã khuyến mãi đã tồn tại");
            }
            existingPromotion.setCode(code);
            existingPromotion.setDiscountPercentage(discountPercentage);
            existingPromotion.setExpirationDate(LocalDateTime.parse(expirationDate, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
            existingPromotion.setCourseID(courseID);
            existingPromotion.setStatus(status);
            existingPromotion.setUsageLimit(usageLimit);
            promotionService.updatePromotion(existingPromotion);
            logger.info("Cập nhật khuyến mãi thành công: {}", promotionID);
            return ResponseEntity.ok("Cập nhật khuyến mãi thành công!");
        } catch (Exception e) {
            logger.error("Lỗi khi cập nhật khuyến mãi: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Không thể cập nhật khuyến mãi: " + e.getMessage());
        }
    }



    @GetMapping("/staffsList")
    public String staffList(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) Integer status,
            Model model) {
        logger.info("Bắt đầu xử lý /owner/staffsList: page={}, size={}, search={}, status={}", page, size, search, status);

        try {
            List<users> staffList = userService.getStaffPaginated(page, size, search, status);
            long totalStaff = userService.countStaff(search, status);
            int totalPages = (int) Math.ceil((double) totalStaff / size);

            model.addAttribute("staffList", staffList);
            model.addAttribute("hasStaff", !staffList.isEmpty());
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalStaff", totalStaff);
            model.addAttribute("search", search);
            model.addAttribute("status", status);
            logger.info("Danh sách nhân viên: {}, tổng số: {}", staffList.size(), totalStaff);
        } catch (Exception e) {
            logger.error("Lỗi khi lấy danh sách nhân viên: {}", e.getMessage());
            model.addAttribute("error", "Không thể tải danh sách nhân viên. Vui lòng thử lại sau.");
        }
        return "owner/staffsList";
    }

    @PostMapping("/staff/update")
    public ResponseEntity<String> updateStaff(@ModelAttribute users updatedUser) {
        try {
            logger.info("Received staff update request for userID: {}", updatedUser.getUserID());

            // Validate required fields
            if (updatedUser.getUserID() <= 0) {
                logger.error("Invalid User ID: {}", updatedUser.getUserID());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid User ID");
            }
            if (updatedUser.getFullname() == null || updatedUser.getFullname().trim().isEmpty()) {
                logger.error("Full name is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Full name is required");
            }
            if (updatedUser.getEmail() == null || updatedUser.getEmail().trim().isEmpty()) {
                logger.error("Email is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email is required");
            }
            // Validate email format
            if (!updatedUser.getEmail().matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
                logger.error("Invalid email format: {}", updatedUser.getEmail());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid email format");
            }
            if (updatedUser.getPhoneNumber() == null || updatedUser.getPhoneNumber().trim().isEmpty()) {
                logger.error("Phone number is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Phone number is required");
            }
            // Validate phone number format
            if (!updatedUser.getPhoneNumber().matches("^\\+?\\d{10,15}$")) {
                logger.error("Invalid phone number format: {}", updatedUser.getPhoneNumber());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid phone number format (must be 10-15 digits, optionally starting with +)");
            }

            // Validate Gender
            if (updatedUser.getGender() == null || updatedUser.getGender().trim().isEmpty()) {
                logger.error("Gender is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Gender is required");
            }
            if (!updatedUser.getGender().equals("MALE") && !updatedUser.getGender().equals("FEMALE") && !updatedUser.getGender().equals("OTHER")) {
                logger.error("Invalid Gender: {}. Must be 'MALE', 'FEMALE', or 'OTHER'", updatedUser.getGender());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid Gender. Must be 'MALE', 'FEMALE', or 'OTHER'");
            }

            // Validate Status
            if (updatedUser.getStatus() != 0 && updatedUser.getStatus() != 1) {
                logger.error("Invalid Status: {}. Must be 0 or 1", updatedUser.getStatus());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid Status. Must be 0 or 1");
            }

            // Fetch existing user
            users existingUser = userService.findByUid(String.valueOf(updatedUser.getUserID()));
            if (existingUser == null) {
                logger.error("User not found with ID: {}", updatedUser.getUserID());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }

            // Update fields
            existingUser.setFullname(updatedUser.getFullname());
            existingUser.setEmail(updatedUser.getEmail());
            existingUser.setPhoneNumber(updatedUser.getPhoneNumber());
            existingUser.setGender(updatedUser.getGender());
            existingUser.setAddress(updatedUser.getAddress());
            existingUser.setStatus(updatedUser.getStatus());

            // Save updated user
            userService.updateUser(existingUser);
            logger.info("Staff updated successfully: userID={}", updatedUser.getUserID());
            return ResponseEntity.ok("Staff updated successfully");
        } catch (Exception e) {
            logger.error("Error updating staff: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating staff: " + e.getMessage());
        }
    }

    @GetMapping("/staff/add")
    public String addStaffForm(Model model) {
        logger.info("Displaying Add Staff form");
        return "owner/addStaff";
    }

    @PostMapping("/staff/add")
    public ResponseEntity<String> addStaff(@ModelAttribute users newUser) {
        try {
            logger.info("Received staff add request: email={}", newUser.getEmail());

            // Validate required fields
            if (newUser.getFullname() == null || newUser.getFullname().trim().isEmpty()) {
                logger.error("Full name is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Full name is required");
            }
            if (newUser.getEmail() == null || newUser.getEmail().trim().isEmpty()) {
                logger.error("Email is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email is required");
            }
            if (!newUser.getEmail().matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
                logger.error("Invalid email format: {}", newUser.getEmail());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid email format");
            }
            if (newUser.getPhoneNumber() == null || newUser.getPhoneNumber().trim().isEmpty()) {
                logger.error("Phone number is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Phone number is required");
            }
            if (!newUser.getPhoneNumber().matches("^\\+?\\d{10,15}$")) {
                logger.error("Invalid phone number format: {}", newUser.getPhoneNumber());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid phone number format (must be 10-15 digits, optionally starting with +)");
            }
            if (newUser.getGender() == null || newUser.getGender().trim().isEmpty()) {
                logger.error("Gender is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Gender is required");
            }
            if (!newUser.getGender().equals("MALE") && !newUser.getGender().equals("FEMALE") && !newUser.getGender().equals("OTHER")) {
                logger.error("Invalid Gender: {}. Must be 'MALE', 'FEMALE', or 'OTHER'", newUser.getGender());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid Gender. Must be 'MALE', 'FEMALE', or 'OTHER'");
            }
            if (newUser.getPassword() == null || newUser.getPassword().trim().isEmpty()) {
                logger.error("Password is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Password is required");
            }

            // Check if email already exists
            try {
                userService.findByEmailOrPhone(newUser.getEmail());
                logger.error("Email already registered: {}", newUser.getEmail());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email already registered");
            } catch (Exception e) {
                // Email not found, continue
            }

            // Log password before trimming
            logger.info("Raw password before trimming: '{}'", newUser.getPassword());

            // Trim password to remove any leading/trailing spaces
            String trimmedPassword = newUser.getPassword().trim();
            newUser.setPassword(trimmedPassword);
            logger.info("Password after trimming: '{}'", newUser.getPassword());

            // Set default role to Staff
            role staffRole = new role();
            staffRole.setRoleID("Staff");
            newUser.setRole(staffRole);

            // Set default status to Active (1)
            newUser.setStatus(1);

            // Save new user (encoding will be handled in UserService.registerUser)
            userService.registerUser(newUser);
            logger.info("Staff added successfully: email={}", newUser.getEmail());
            return ResponseEntity.ok("Staff added successfully");
        } catch (Exception e) {
            logger.error("Error adding staff: {}", e.getMessage(), e);
            String errorMessage = e.getMessage().contains("Số điện thoại đã được sử dụng")
                    ? e.getMessage()
                    : "Không thể thêm nhân viên: " + e.getMessage();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
        }
    }

}

class CourseUpdateDTO {
    private int courseID;
    private String title;
    private String description;
    private BigDecimal prices;
    private String status;
    private String image;
    private int quantity;
    private CategoryDTO category;

    public int getCourseID() { return courseID; }
    public void setCourseID(int courseID) { this.courseID = courseID; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrices() { return prices; }
    public void setPrices(BigDecimal prices) { this.prices = prices; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public CategoryDTO getCategory() { return category; }
    public void setCategory(CategoryDTO category) { this.category = category; }

    @Override
    public String toString() {
        return "CourseUpdateDTO{" +
                "courseID=" + courseID +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", prices=" + prices +
                ", status='" + status + '\'' +
                ", image='" + image + '\'' +
                ", quantity=" + quantity +
                ", category=" + category +
                '}';
    }
}

class CategoryDTO {
    private int categoryID;

    public int getCategoryID() { return categoryID; }
    public void setCategoryID(int categoryID) { this.categoryID = categoryID; }

    @Override
    public String toString() {
        return "CategoryDTO{categoryID=" + categoryID + '}';
    }
}