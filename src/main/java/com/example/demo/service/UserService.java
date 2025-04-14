package com.example.demo.service;

import com.example.demo.model.role;
import com.example.demo.model.users;
import com.example.demo.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

@Service
public class UserService implements UserDetailsService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.info("Loading user by username: {}", username);
        users user = userRepository.findByEmail(username)
                .orElseGet(() -> userRepository.findByPhoneNumber(username)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username)));

        // Create authorities list with user's role
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (user.getRole() != null) {
            String roleAuthority = "ROLE_" + user.getRole().getRoleID();
            authorities.add(new SimpleGrantedAuthority(roleAuthority));
            logger.info("Added authority {} for user {}", roleAuthority, username);
        } else {
            logger.warn("No role found for user {}", username);
        }

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                true, 
                true, 
                true, 
                true, 
                authorities
        );
    }

    public users registerUser(users user) {
        
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        if (userRepository.findByPhoneNumber(user.getPhoneNumber()).isPresent()) {
            throw new RuntimeException("Phone number already exists");
        }

        
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        return userRepository.save(user);
    }

    public users authenticateUser(String emailOrPhone, String password) {
        logger.info("Authenticating user with email/phone: {}", emailOrPhone);
        users user = findByEmailOrPhone(emailOrPhone);
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            logger.error("Invalid password for user: {}", emailOrPhone);
            throw new UsernameNotFoundException("Invalid credentials");
        }
        
        if (user.getRole() == null) {
            logger.warn("User {} authenticated but has no role", emailOrPhone);
        } else {
            logger.info("User {} authenticated successfully with role {}", emailOrPhone, user.getRole().getRoleID());
        }
        
        return user;
    }

    public users findByEmailOrPhone(String emailOrPhone) {
        logger.info("Finding user by email/phone: {}", emailOrPhone);
        return userRepository.findByEmail(emailOrPhone)
                .orElseGet(() -> userRepository.findByPhoneNumber(emailOrPhone)
                        .orElseThrow(() -> {
                            logger.error("User not found with email/phone: {}", emailOrPhone);
                            return new UsernameNotFoundException("User not found");
                        }));
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
        // Check if user already exists
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

        // Set default role as CUSTOMER for new users
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
            if (!user.getPhoneNumber().startsWith("+")) {
                user.setPhoneNumber("+84" + user.getPhoneNumber().replaceAll("^0", ""));
            }
        }

        // Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        try {
            // Save user to database
            users savedUser = userRepository.save(user);
            logger.info("Successfully saved user: {} with role: {}", 
                savedUser.getEmail(), 
                savedUser.getRole() != null ? savedUser.getRole().getRoleID() : "no role");
            return savedUser;
        } catch (Exception e) {
            logger.error("Failed to save user: {}", e.getMessage());
            throw new RuntimeException("Failed to save user: " + e.getMessage());
        }
    }
} 