package com.example.demo.repository;

import com.example.demo.model.users;
import com.example.demo.model.role;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class UserRepository {
    private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);
    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<users> userRowMapper = (ResultSet rs, int rowNum) -> {
        users user = new users();
        try {
            user.setUserID(rs.getInt("UserID"));
            user.setEmail(rs.getString("Email"));
            user.setPhoneNumber(rs.getString("PhoneNumber"));
            user.setFullname(rs.getString("Fullname"));
            user.setAddress(rs.getString("Address"));
            user.setGender(rs.getString("Gender"));
            user.setPassword(rs.getString("Password"));
            user.setEmailCode(rs.getString("EmailCode"));
            user.setCreateDate(rs.getTimestamp("CreateDate").toLocalDateTime());
            user.setUpdateDate(rs.getTimestamp("UpdateDate").toLocalDateTime());
            
            // Map role information
            String roleId = rs.getString("RoleID");
            String roleName = rs.getString("RoleName");
            
            if (roleId != null) {
                role userRole = new role();
                userRole.setRoleID(roleId);
                userRole.setRoleName(roleName != null ? roleName : roleId);
                user.setRole(userRole);
                logger.info("Mapped role for user {}: roleID={}, roleName={}", user.getEmail(), roleId, roleName);
            } else {
                logger.warn("No role found for user {}", user.getEmail());
            }
            
        } catch (SQLException e) {
            logger.error("Error mapping user data: {}", e.getMessage());
            throw e;
        }
        return user;
    };

    public Optional<users> findByEmail(String email) {
        String sql = "SELECT u.*, r.RoleName FROM users u " +
                    "LEFT JOIN roles r ON u.RoleID = r.RoleID " +
                    "WHERE u.Email = ?";
        try {
            logger.info("Finding user by email: {}", email);
            users user = jdbcTemplate.queryForObject(sql, userRowMapper, email);
            if (user != null) {
                logger.info("Found user: {}, role: {}", user.getEmail(), 
                    user.getRole() != null ? user.getRole().getRoleID() : "no role");
            }
            return Optional.ofNullable(user);
        } catch (Exception e) {
            logger.error("Error finding user by email {}: {}", email, e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<users> findByPhoneNumber(String phoneNumber) {
        String sql = "SELECT u.*, r.RoleName FROM users u " +
                    "LEFT JOIN roles r ON u.RoleID = r.RoleID " +
                    "WHERE u.PhoneNumber = ?";
        try {
            logger.info("Finding user by phone number: {}", phoneNumber);
            users user = jdbcTemplate.queryForObject(sql, userRowMapper, phoneNumber);
            if (user != null) {
                logger.info("Found user: {}, role: {}", user.getPhoneNumber(), 
                    user.getRole() != null ? user.getRole().getRoleID() : "no role");
            }
            return Optional.ofNullable(user);
        } catch (Exception e) {
            logger.error("Error finding user by phone number {}: {}", phoneNumber, e.getMessage());
            return Optional.empty();
        }
    }
    
    public Optional<users> findByUid(String uid) {
        String sql = "SELECT u.*, r.RoleName FROM users u " +
                    "LEFT JOIN roles r ON u.RoleID = r.RoleID " +
                    "WHERE u.UserID = ?";
        try {
            logger.info("Finding user by UID: {}", uid);
            users user = jdbcTemplate.queryForObject(sql, userRowMapper, Integer.parseInt(uid));
            if (user != null) {
                logger.info("Found user by UID: {}, role: {}", uid, 
                    user.getRole() != null ? user.getRole().getRoleID() : "no role");
            }
            return Optional.ofNullable(user);
        } catch (Exception e) {
            logger.error("Error finding user by UID {}: {}", uid, e.getMessage());
            return Optional.empty();
        }
    }

    public users save(users user) {
        try {
            if (user.getUserID() == 0) {
                logger.info("Creating new user: {}", user.getEmail());
                // Insert new user
                String sql = "INSERT INTO users (Email, PhoneNumber, Fullname, Address, Gender, Password, RoleID, EmailCode, CreateDate, UpdateDate) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                jdbcTemplate.update(sql,
                    user.getEmail(),
                    user.getPhoneNumber(),
                    user.getFullname(),
                    user.getAddress(),
                    user.getGender(),
                    user.getPassword(),
                    user.getRole() != null ? user.getRole().getRoleID() : null,
                    user.getEmailCode(),
                    LocalDateTime.now(),
                    LocalDateTime.now()
                );
                
                // Get the newly inserted user's ID
                String getIdSql = "SELECT UserID FROM users WHERE Email = ?";
                int userId = jdbcTemplate.queryForObject(getIdSql, Integer.class, user.getEmail());
                user.setUserID(userId);
                
                logger.info("Created new user with ID: {} and role: {}", userId, 
                    user.getRole() != null ? user.getRole().getRoleID() : "no role");
                
                return user;
            } else {
                logger.info("Updating existing user: {}", user.getEmail());
                // Update existing user
                String sql = "UPDATE users SET Email = ?, PhoneNumber = ?, Fullname = ?, Address = ?, Gender = ?, " +
                            "Password = ?, RoleID = ?, EmailCode = ?, UpdateDate = ? WHERE UserID = ?";
                jdbcTemplate.update(sql,
                    user.getEmail(),
                    user.getPhoneNumber(),
                    user.getFullname(),
                    user.getAddress(),
                    user.getGender(),
                    user.getPassword(),
                    user.getRole() != null ? user.getRole().getRoleID() : null,
                    user.getEmailCode(),
                    LocalDateTime.now(),
                    user.getUserID()
                );
                
                logger.info("Updated user {} with role: {}", user.getEmail(),
                    user.getRole() != null ? user.getRole().getRoleID() : "no role");
                
                return user;
            }
        } catch (Exception e) {
            logger.error("Error saving user {}: {}", user.getEmail(), e.getMessage());
            throw e;
        }
    }
} 