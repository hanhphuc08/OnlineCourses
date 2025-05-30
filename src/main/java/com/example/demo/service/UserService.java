package com.example.demo.service;

import com.example.demo.model.order;
import com.example.demo.model.role;
import com.example.demo.model.users;
import com.example.demo.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private OrderService orderService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.info("Loading user by username: {}", username);
        try {
            users user = findByEmailOrPhone(username);
            return new CustomUserDetails(user);
        } catch (UsernameNotFoundException e) {
            logger.error("User not found: {}", username);
            throw e;
        }
    }

    // Custom UserDetails implementation
    private static class CustomUserDetails implements UserDetails {
        private final users user;

        public CustomUserDetails(users user) {
            this.user = user;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            if (user.getRole() != null) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().getRoleID()));
            }
            return authorities;
        }

        @Override
        public String getPassword() {
            return user.getPassword();
        }

        @Override
        public String getUsername() {
            return user.getEmail();
        }

        @Override
        public boolean isAccountNonExpired() {
            return true;
        }

        @Override
        public boolean isAccountNonLocked() {
            return true;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        // Custom getters for user information
        public String getFullname() {
            return user.getFullname();
        }

        public String getEmail() {
            return user.getEmail();
        }

        public String getPhoneNumber() {
            return user.getPhoneNumber();
        }

        public role getRole() {
            return user.getRole();
        }
    }

    public users registerUser(users user) {
        try {
            // Check if user exists
            findByEmailOrPhone(user.getEmail());
            throw new RuntimeException("Email đã được sử dụng");
        } catch (UsernameNotFoundException e) {
            // Email not found, continue
        }

        try {
            findByEmailOrPhone(user.getPhoneNumber());
            throw new RuntimeException("Số điện thoại đã được sử dụng");
        } catch (UsernameNotFoundException e) {
            // Phone number not found, continue
        }

        // Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Save user
        users savedUser = userRepository.save(user);
        logger.info("Successfully registered user: {}", savedUser.getEmail());
        return savedUser;
    }

    public users authenticateUser(String emailOrPhone, String password) {
        logger.info("Authenticating user with email/phone: {}", emailOrPhone);
        try {
            users user = findByEmailOrPhone(emailOrPhone);
            
            if (!passwordEncoder.matches(password, user.getPassword())) {
                logger.error("Invalid password for user: {}", emailOrPhone);
                throw new UsernameNotFoundException("Invalid credentials");
            }

            if (user.getStatus() != 1) {
                logger.error("Inactive account attempt for user: {}", emailOrPhone);
                throw new UsernameNotFoundException("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ admin để được hỗ trợ.");
            }
            
            logger.info("User {} authenticated successfully with role {}", 
                emailOrPhone, 
                user.getRole() != null ? user.getRole().getRoleID() : "no role");
            
            throw new RuntimeException("Đăng nhập thành công! Chào mừng bạn quay trở lại.");
        } catch (Exception e) {
            if (e.getMessage().equals("Đăng nhập thành công! Chào mừng bạn quay trở lại.")) {
                throw e;
            }
            logger.error("Authentication failed for user {}: {}", emailOrPhone, e.getMessage());
            throw new UsernameNotFoundException("Invalid credentials");
        }
    }

    public users findByEmailOrPhone(String emailOrPhone) {
        logger.info("Finding user by email/phone: {}", emailOrPhone);
        
        Optional<users> userByEmail = userRepository.findByEmail(emailOrPhone);
        if (userByEmail.isPresent()) {
            return userByEmail.get();
        }
        
        Optional<users> userByPhone = userRepository.findByPhoneNumber(emailOrPhone);
        if (userByPhone.isPresent()) {
            return userByPhone.get();
        }
        
        logger.error("User not found with email/phone: {}", emailOrPhone);
        throw new UsernameNotFoundException("Không tìm thấy tài khoản với email hoặc số điện thoại: " + emailOrPhone);
    }
    
    public users findByUid(String uid) {
        logger.info("Finding user by UID: {}", uid);
        return userRepository.findByUid(uid)
                .orElseThrow(() -> {
                    logger.error("User not found with UID: {}", uid);
                    return new UsernameNotFoundException("User not found with UID: " + uid);
                });
    }
    
    public users saveUser(users user) {
        // Nếu user đã có ID, đây là cập nhật
        if (user.getUserID() != 0) {
            // Cập nhật thời gian
            user.setUpdateDate(LocalDateTime.now());
            try {
                // Lưu user vào database
                users savedUser = userRepository.save(user);
                logger.info("Successfully updated user: {} with role: {}", 
                    savedUser.getEmail(), 
                    savedUser.getRole() != null ? savedUser.getRole().getRoleID() : "no role");
                return savedUser;
            } catch (Exception e) {
                logger.error("Failed to update user: {}", e.getMessage());
                throw new RuntimeException("Failed to update user: " + e.getMessage());
            }
        }

        // Nếu là user mới, kiểm tra email và phone đã tồn tại chưa
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            logger.error("Email already exists: {}", user.getEmail());
            throw new RuntimeException("Email already exists");
        }
        if (userRepository.findByPhoneNumber(user.getPhoneNumber()).isPresent()) {
            logger.error("Phone number already exists: {}", user.getPhoneNumber());
            throw new RuntimeException("Phone number already exists");
        }

        // Validate required fields
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new RuntimeException("Email is required");
        }
        if (user.getFullname() == null || user.getFullname().trim().isEmpty()) {
            throw new RuntimeException("Full name is required");
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new RuntimeException("Password is required");
        }

        // Set default role if not set
        if (user.getRole() == null) {
            role customerRole = new role();
            customerRole.setRoleID("Customer");
            customerRole.setRoleName("Customer");
            user.setRole(customerRole);
            logger.info("Setting default Customer role for new user: {}", user.getEmail());
        }

        // Set default values for optional fields
        if (user.getGender() == null || user.getGender().trim().isEmpty()) {
            user.setGender("MALE");
        } else {
            String gender = user.getGender().toUpperCase();
            if (!gender.equals("MALE") && !gender.equals("FEMALE") && !gender.equals("OTHER")) {
                throw new RuntimeException("Gender must be MALE, FEMALE, or OTHER");
            }
            user.setGender(gender);
        }
        
        if (user.getAddress() == null || user.getAddress().trim().isEmpty()) {
            user.setAddress("");
        }
        if (user.getEmailCode() == null || user.getEmailCode().trim().isEmpty()) {
            user.setEmailCode("");
        }
        
        // Set current date for createDate and updateDate
        LocalDateTime currentDateTime = LocalDateTime.now();
        user.setCreateDate(currentDateTime);
        user.setUpdateDate(currentDateTime);
        
        // Ensure phone number is in correct format
        if (user.getPhoneNumber() != null) {
            String phone = user.getPhoneNumber();
            if (phone.startsWith("+84")) {
                user.setPhoneNumber("0" + phone.substring(3));
            } else if (phone.startsWith("84")) {
                user.setPhoneNumber("0" + phone.substring(2));
            } else if (phone.startsWith("0")) {
                user.setPhoneNumber(phone);
            } else {
                user.setPhoneNumber("0" + phone);
            }
        }

        // Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        try {
            // Save user to database
            users savedUser = userRepository.save(user);
            logger.info("Successfully saved new user: {} with role: {}", 
                savedUser.getEmail(), 
                savedUser.getRole() != null ? savedUser.getRole().getRoleID() : "no role");
            return savedUser;
        } catch (Exception e) {
            logger.error("Failed to save user: {}", e.getMessage());
            throw new RuntimeException("Failed to save user: " + e.getMessage());
        }
    }

    public String generateResetCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }

    public void saveResetCode(users user, String resetCode) {
        user.setEmailCode(resetCode);
        user.setUpdateDate(LocalDateTime.now());
        userRepository.save(user);
    }

    public boolean verifyResetCode(String email, String resetCode) {
        Optional<users> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            users user = userOpt.get();
            LocalDateTime codeCreationTime = user.getUpdateDate();
            LocalDateTime now = LocalDateTime.now();        
            if (codeCreationTime != null && 
                now.minusMinutes(1).isBefore(codeCreationTime)) {
                return resetCode.equals(user.getEmailCode());
            }
        }
        return false;
    }

    public void resetPassword(String email, String newPassword) {
        Optional<users> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            users user = userOpt.get();
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setEmailCode(null); 
            user.setUpdateDate(LocalDateTime.now());
            userRepository.save(user);
        }
    }
    
    public void updateUser(users user) {
        userRepository.updateUser(user);
    }
    
    public boolean checkPassword(users user, String rawPassword) {
        logger.info("Checking password for user: {}", user.getEmail());
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    public void updatePassword(users user, String newPassword) {
        logger.info("Updating password for user: {}", user.getEmail());
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateDate(LocalDateTime.now());
        userRepository.save(user);
        throw new RuntimeException("Đổi mật khẩu thành công! Vui lòng đăng nhập lại với mật khẩu mới.");
    }

    public List<users> findAllCustomers() {
        logger.info("Fetching all customers with role Customer");
        List<users> customerList = userRepository.findAllByRoleID("Customer");
        logger.info("Found {} customers", customerList.size());
        if (customerList.isEmpty()) {
            logger.warn("No customers found with role Customer in the database.");
        } else {
            customerList.forEach(user -> logger.info("Customer: {}, Role: {}", user.getEmail(), user.getRole().getRoleID()));
        }
        return customerList;
    }

    public List<users> findAllStaff() {
        logger.info("Fetching all staff members with role Staff");
        List<users> staffList = userRepository.findAllByRoleID("Staff");
        logger.info("Found {} staff members", staffList.size());
        if (staffList.isEmpty()) {
            logger.warn("No staff members found. Checking database for Staff users...");
            List<users> allUsers = userRepository.findAllByRoleID("Staff");
            logger.info("Total users with Staff role: {}", allUsers.size());
            allUsers.forEach(user -> logger.info("User: {}, Role: {}", user.getEmail(), user.getRole().getRoleID()));
        }
        return staffList;
    }
    
    public List<users> findAllStaffByStatus(Integer status) {
        logger.info("Fetching staff members with role Staff and status: {}", status == null ? "all" : status);
        List<users> staffList = userRepository.findAllStaffByRoleAndStatus("Staff", status);
        logger.info("Found {} staff members", staffList.size());
        if (staffList.isEmpty()) {
            logger.warn("No staff members found with status: {}", status == null ? "all" : status);
        } else {
            staffList.forEach(user -> logger.info("Staff: {}, Role: {}, Status: {}", user.getEmail(), user.getRole().getRoleID(), user.getStatus()));
        }
        return staffList;
    }
    
    public long countAllStudents() {
        logger.info("Đếm tổng số học viên");
        return userRepository.countAllStudents();
    }


    public List<users> getCustomersPaginated(int page, int size, String search, Integer status) {
        logger.info("Lấy danh sách khách hàng với phân trang: page={}, size={}, search={}, status={}", page, size, search, status);
        List<users> customers = userRepository.findCustomersPaginated(page, size, search, status);

        for (users customer : customers) {
            List<order> orders = orderService.findByUserId(customer.getUserID());
            customer.setOrders(orders);
        }

        return customers;
    }

    public long countCustomers(String search, Integer status) {
        logger.info("Đếm số khách hàng: search={}, status={}", search, status);
        return userRepository.countCustomers(search, status);
    }

    public List<users> getStaffPaginated(int page, int size, String search, Integer status) {
        logger.info("Lấy danh sách nhân viên với phân trang: page={}, size={}, search={}, status={}", page, size, search, status);
        return userRepository.findStaffPaginated(page, size, search, status);
    }

    public long countStaff(String search, Integer status) {
        logger.info("Đếm số nhân viên: search={}, status={}", search, status);
        return userRepository.countStaff(search, status);
    }
    public String generateVerificationCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }

    public void deleteUser(int userId) {
        logger.info("Deleting user with ID: {}", userId);
        try {
            userRepository.deleteById(userId);
            logger.info("Successfully deleted user with ID: {}", userId);
        } catch (Exception e) {
            logger.error("Error deleting user with ID {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to delete user: " + e.getMessage());
        }
    }
} 