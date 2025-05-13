package com.example.demo.repository;

import com.example.demo.model.users;
import com.example.demo.model.role;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
            user.setStatus(rs.getInt("Status"));
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
                return Optional.of(user);
            }
            logger.error("No user found with email: {}", email);
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Error finding user by email {}: {}", email, e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<users> findByPhoneNumber(String phoneNumber) {
        String sql = "SELECT u.*, r.RoleName FROM users u " +
                    "LEFT JOIN roles r ON u.RoleID = r.RoleID " +
                    "WHERE u.PhoneNumber = ? OR u.PhoneNumber = ? OR u.PhoneNumber = ? OR u.PhoneNumber = ?";
        try {
            logger.info("Finding user by phone number: {}", phoneNumber);
            // Remove any potential formatting
            String cleanPhone = phoneNumber.replaceAll("[^0-9]", "");
            
            // Try different formats
            String withZero = cleanPhone.startsWith("0") ? cleanPhone : "0" + cleanPhone;
            String with84 = cleanPhone.startsWith("84") ? cleanPhone : "84" + cleanPhone.substring(1);
            String withPlus84 = "+" + with84;
            String justNumbers = cleanPhone.startsWith("0") ? cleanPhone.substring(1) : cleanPhone;
            
            users user = jdbcTemplate.queryForObject(sql, userRowMapper, 
                withZero, with84, withPlus84, justNumbers);
            
            if (user != null) {
                logger.info("Found user: {}, role: {}", user.getPhoneNumber(), 
                    user.getRole() != null ? user.getRole().getRoleID() : "no role");
                return Optional.of(user);
            }
            logger.error("No user found with phone number: {} (tried formats: {}, {}, {}, {})", 
                phoneNumber, withZero, with84, withPlus84, justNumbers);
            return Optional.empty();
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
                String sql = "INSERT INTO users (Email, PhoneNumber, Fullname, Address, Gender, Password, RoleID, EmailCode, CreateDate, UpdateDate, Status) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
                    LocalDateTime.now(),
                    user.getStatus()
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
                            "Password = ?, RoleID = ?, EmailCode = ?, UpdateDate = ?, Status = ? WHERE UserID = ?";
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
                    user.getStatus(),
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
    public void updateUserProfile(int userId, String fullname, String email, String phoneNumber) {
        String sql = "UPDATE users SET Fullname = ?, Email = ?, PhoneNumber = ?, UpdateDate = ? WHERE UserID = ?";
        try {
            jdbcTemplate.update(sql, fullname, email, phoneNumber, LocalDateTime.now(), userId);
            logger.info("Cập nhật hồ sơ người dùng thành công cho userId: {}", userId);
        } catch (Exception e) {
            logger.error("Lỗi khi cập nhật hồ sơ người dùng cho userId {}: {}", userId, e.getMessage());
            throw new RuntimeException("Không thể cập nhật hồ sơ người dùng: " + e.getMessage());
        }
    }

    public void updateUser(users user) {
        String sql = "UPDATE users SET fullname = ?, phoneNumber = ?, address = ?, gender = ?, status = ?, updateDate = ? WHERE userID = ?";
        try {
            int rows = jdbcTemplate.update(sql,
                    user.getFullname(),
                    user.getPhoneNumber(),
                    user.getAddress(),
                    user.getGender(),
                    user.getStatus(),
                    LocalDateTime.now(),
                    user.getUserID()
            );
            if (rows > 0) {
                logger.info("Cập nhật thông tin người dùng thành công: userID={}", user.getUserID());
            } else {
                logger.warn("Không tìm thấy người dùng để cập nhật: userID={}", user.getUserID());
                throw new RuntimeException("Không tìm thấy người dùng để cập nhật");
            }
        } catch (Exception e) {
            logger.error("Lỗi khi cập nhật thông tin người dùng: userID={}, lỗi: {}", user.getUserID(), e.getMessage());
            throw new RuntimeException("Lỗi khi cập nhật thông tin: " + e.getMessage());
        }
    }

    public List<users> findAllByRoleID(String roleId) {
        String sql = "SELECT u.*, r.RoleName FROM users u JOIN roles r ON u.RoleID = r.RoleID WHERE r.RoleID = ?";
        try {
            logger.info("Executing query to find users with roleID: {}", roleId);
            List<users> users = jdbcTemplate.query(sql, new Object[]{roleId}, userRowMapper);
            logger.info("Found {} users with roleID: {}", users.size(), roleId);
            if (users.isEmpty()) {
                logger.warn("No users found with roleID: {}. Checking query and data...", roleId);
            } else {
                users.forEach(user -> logger.info("User: {}, Role: {}", user.getEmail(), user.getRole().getRoleID()));
            }
            return users;
        } catch (Exception e) {
            logger.error("Error finding users by roleID {}: {}", roleId, e.getMessage());
            return new ArrayList<>();
        }
    }
    public List<users> findAllStaffByRoleAndStatus(String roleId, Integer status) {
        String sql = "SELECT u.*, r.RoleName FROM users u JOIN roles r ON u.RoleID = r.RoleID WHERE r.RoleID = ?";
        List<Object> params = new ArrayList<>();
        params.add(roleId);
        
        if (status != null) {
            sql += " AND u.Status = ?";
            params.add(status);
        }
        
        sql += " ORDER BY u.UserID";
        
        try {
            logger.info("Executing query to find staff with roleID: {} and status: {}", roleId, status == null ? "all" : status);
            List<users> users = jdbcTemplate.query(sql, params.toArray(), userRowMapper);
            logger.info("Found {} staff with roleID: {} and status: {}", users.size(), roleId, status == null ? "all" : status);
            if (users.isEmpty()) {
                logger.warn("No staff found with roleID: {} and status: {}. Checking query and data...", roleId, status == null ? "all" : status);
            } else {
                users.forEach(user -> logger.info("Staff: {}, Role: {}, Status: {}", user.getEmail(), user.getRole().getRoleID(), user.getStatus()));
            }
            return users;
        } catch (Exception e) {
            logger.error("Error finding staff by roleID {} and status {}: {}", roleId, status == null ? "all" : status, e.getMessage());
            return new ArrayList<>();
        }
    }
    
    public long countAllStudents() {
        String sql = "SELECT COUNT(*) FROM users WHERE RoleID = 'CUSTOMER'";
        try {
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            logger.info("Đếm tổng số học viên: {}", count);
            return count != null ? count : 0;
        } catch (Exception e) {
            logger.error("Lỗi khi đếm tổng số học viên: {}", e.getMessage());
            throw new RuntimeException("Lỗi khi đếm tổng số học viên: " + e.getMessage());
        }
    }
    // Thêm phương thức để lấy danh sách khách hàng với phân trang và bộ lọc
    public List<users> findCustomersPaginated(int page, int size, String search, Integer status) {
        StringBuilder sql = new StringBuilder(
                "SELECT u.*, r.RoleName " +
                        "FROM users u " +
                        "JOIN roles r ON u.RoleID = r.RoleID " +
                        "WHERE r.RoleID = 'Customer' "
        );

        List<Object> params = new ArrayList<>();
        boolean hasWhereClause = true; // Đã có điều kiện WHERE r.RoleID = 'Customer'

        // Thêm điều kiện tìm kiếm theo từ khóa
        if (search != null && !search.trim().isEmpty()) {
            sql.append(hasWhereClause ? " AND " : " WHERE ");
            sql.append("(LOWER(u.Fullname) LIKE ? OR LOWER(u.Email) LIKE ?)");
            params.add("%" + search.toLowerCase() + "%");
            params.add("%" + search.toLowerCase() + "%");
            hasWhereClause = true;
        }

        // Thêm điều kiện lọc theo trạng thái
        if (status != null) {
            sql.append(hasWhereClause ? " AND " : " WHERE ");
            sql.append("u.Status = ?");
            params.add(status);
        }

        // Thêm phân trang
        sql.append(" ORDER BY u.UserID LIMIT ? OFFSET ?");
        params.add(size);
        params.add(page * size);

        try {
            logger.info("Executing query to find customers: {}", sql.toString());
            return jdbcTemplate.query(sql.toString(), params.toArray(), userRowMapper);
        } catch (Exception e) {
            logger.error("Error fetching paginated customers: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // Thêm phương thức để đếm tổng số khách hàng với bộ lọc
    public long countCustomers(String search, Integer status) {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) " +
                        "FROM users u " +
                        "JOIN roles r ON u.RoleID = r.RoleID " +
                        "WHERE r.RoleID = 'Customer' "
        );

        List<Object> params = new ArrayList<>();
        boolean hasWhereClause = true; // Đã có điều kiện WHERE r.RoleID = 'Customer'

        if (search != null && !search.trim().isEmpty()) {
            sql.append(hasWhereClause ? " AND " : " WHERE ");
            sql.append("(LOWER(u.Fullname) LIKE ? OR LOWER(u.Email) LIKE ?)");
            params.add("%" + search.toLowerCase() + "%");
            params.add("%" + search.toLowerCase() + "%");
            hasWhereClause = true;
        }

        if (status != null) {
            sql.append(hasWhereClause ? " AND " : " WHERE ");
            sql.append("u.Status = ?");
            params.add(status);
        }

        try {
            Long count = jdbcTemplate.queryForObject(sql.toString(), params.toArray(), Long.class);
            logger.info("Counted {} customers", count);
            return count != null ? count : 0;
        } catch (Exception e) {
            logger.error("Error counting customers: {}", e.getMessage());
            throw new RuntimeException("Error counting customers: " + e.getMessage());
        }
    }

    // Thêm phương thức để lấy danh sách nhân viên với phân trang và bộ lọc
    public List<users> findStaffPaginated(int page, int size, String search, Integer status) {
        StringBuilder sql = new StringBuilder(
                "SELECT u.*, r.RoleName " +
                        "FROM users u " +
                        "JOIN roles r ON u.RoleID = r.RoleID " +
                        "WHERE r.RoleID = 'Staff' "
        );

        List<Object> params = new ArrayList<>();
        boolean hasWhereClause = true; // Đã có điều kiện WHERE r.RoleID = 'Staff'

        // Thêm điều kiện tìm kiếm theo từ khóa
        if (search != null && !search.trim().isEmpty()) {
            sql.append(hasWhereClause ? " AND " : " WHERE ");
            sql.append("(LOWER(u.Fullname) LIKE ? OR LOWER(u.Email) LIKE ?)");
            params.add("%" + search.toLowerCase() + "%");
            params.add("%" + search.toLowerCase() + "%");
            hasWhereClause = true;
        }

        // Thêm điều kiện lọc theo trạng thái
        if (status != null) {
            sql.append(hasWhereClause ? " AND " : " WHERE ");
            sql.append("u.Status = ?");
            params.add(status);
        }

        // Thêm phân trang
        sql.append(" ORDER BY u.UserID LIMIT ? OFFSET ?");
        params.add(size);
        params.add(page * size);

        try {
            logger.info("Executing query to find staff: {}", sql.toString());
            return jdbcTemplate.query(sql.toString(), params.toArray(), userRowMapper);
        } catch (Exception e) {
            logger.error("Error fetching paginated staff: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // Thêm phương thức để đếm tổng số nhân viên với bộ lọc
    public long countStaff(String search, Integer status) {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) " +
                        "FROM users u " +
                        "JOIN roles r ON u.RoleID = r.RoleID " +
                        "WHERE r.RoleID = 'Staff' "
        );

        List<Object> params = new ArrayList<>();
        boolean hasWhereClause = true; // Đã có điều kiện WHERE r.RoleID = 'Staff'

        if (search != null && !search.trim().isEmpty()) {
            sql.append(hasWhereClause ? " AND " : " WHERE ");
            sql.append("(LOWER(u.Fullname) LIKE ? OR LOWER(u.Email) LIKE ?)");
            params.add("%" + search.toLowerCase() + "%");
            params.add("%" + search.toLowerCase() + "%");
            hasWhereClause = true;
        }

        if (status != null) {
            sql.append(hasWhereClause ? " AND " : " WHERE ");
            sql.append("u.Status = ?");
            params.add(status);
        }

        try {
            Long count = jdbcTemplate.queryForObject(sql.toString(), params.toArray(), Long.class);
            logger.info("Counted {} staff", count);
            return count != null ? count : 0;
        } catch (Exception e) {
            logger.error("Error counting staff: {}", e.getMessage());
            throw new RuntimeException("Error counting staff: " + e.getMessage());
        }
    }

    public void deleteById(int userId) {
        String sql = "DELETE FROM users WHERE UserID = ?";
        try {
            int rows = jdbcTemplate.update(sql, userId);
            if (rows > 0) {
                logger.info("Successfully deleted user with ID: {}", userId);
            } else {
                logger.warn("No user found with ID: {}", userId);
                throw new RuntimeException("No user found with ID: " + userId);
            }
        } catch (Exception e) {
            logger.error("Error deleting user with ID {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to delete user: " + e.getMessage());
        }
    }
} 