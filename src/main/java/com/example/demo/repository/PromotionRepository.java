package com.example.demo.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.demo.model.course;
import com.example.demo.model.promotion;

@Repository
public class PromotionRepository {
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	
	private promotion mapRowToPromotion(ResultSet rs, int rowNum) throws SQLException {
        promotion promotion = new promotion();
        promotion.setPromotionID(rs.getInt("PromotionID"));
        promotion.setCode(rs.getString("Code"));
        promotion.setDiscountPercentage(rs.getDouble("DiscountPercentage"));
        promotion.setExpirationDate(rs.getTimestamp("ExpirationDate").toLocalDateTime());
        promotion.setCourseID(rs.getInt("CourseID"));
        promotion.setStatus(rs.getString("Status"));
        promotion.setCreateAt(rs.getTimestamp("CreateAt").toLocalDateTime());
        promotion.setUsageLimit(rs.getInt("UsageLimit"));
        promotion.setUsageCount(rs.getInt("UsageCount"));
        
     // Map course (nếu có)
        course course = new course();
        course.setCourseID(rs.getInt("CourseID"));
        try {
            course.setTitle(rs.getString("CourseTitle"));
        } catch (SQLException e) {
            course.setTitle(null); // Trường CourseTitle có thể không tồn tại trong một số truy vấn
        }
        promotion.setCourse(course);
        return promotion;
    }
	
	public List<promotion> getAllPromotions() {
        String sql = "SELECT p.*, c.Title as CourseTitle " +
                     "FROM promotion p " +
                     "LEFT JOIN course c ON p.CourseID = c.CourseID";
        return jdbcTemplate.query(sql, this::mapRowToPromotion);
    }
	
	public promotion findByCode(String code) {
        String sql = "SELECT p.*, c.Title as CourseTitle " +
                     "FROM promotion p " +
                     "LEFT JOIN course c ON p.CourseID = c.CourseID " +
                     "WHERE p.Code = ?";
        try {
            return jdbcTemplate.queryForObject(sql, new Object[]{code}, this::mapRowToPromotion);
        } catch (Exception e) {
            return null;
        }
    }
	
	public promotion findByCodeAndCourseId(String code, int courseId) {
		
        String sql = "SELECT * FROM promotion WHERE Code = ? AND CourseID = ? AND Status = 'ACTIVE' AND ExpirationDate > NOW()";
        try {
            return jdbcTemplate.queryForObject(sql, new Object[]{code, courseId}, this::mapRowToPromotion);
        } catch (Exception e) {
            return null;
        }
    }
	
	public promotion findById(Integer promotionId) {
        String sql = "SELECT * FROM promotion WHERE PromotionID = ?";
        try {
            return jdbcTemplate.queryForObject(sql, new Object[]{promotionId}, this::mapRowToPromotion);
        } catch (Exception e) {
            return null;
        }
    }
	
	public String validateCoupon(String code, int courseId, int userId) {
		
		String sql = "SELECT * FROM promotion WHERE Code = ? AND CourseID = ? AND Status = 'ACTIVE' AND ExpirationDate > NOW()";
        promotion promotion;
        try {
            promotion = jdbcTemplate.queryForObject(sql, new Object[]{code, courseId}, this::mapRowToPromotion);
        } catch (Exception e) {
            return "Mã giảm giá không hợp lệ hoặc đã hết hạn.";
        }

        String checkUsedSql = "SELECT COUNT(*) FROM userPromotion WHERE PromotionID = ? AND UserID = ?";
        
        int count = jdbcTemplate.queryForObject(checkUsedSql, new Object[]{promotion.getPromotionID(), userId}, Integer.class);
        if (count > 0) {
        	
            return "Bạn đã sử dụng mã giảm giá này rồi.";
        }

        return null;
    }
	
	public void saveUserPromotion(int promotionId, int userId) {
	    String sql = "INSERT INTO userPromotion (PromotionID, UserID, UsedAt) VALUES (?, ?, NOW())";
	    try {
	        jdbcTemplate.update(sql, promotionId, userId);
	    } catch (DataAccessException e) {
	        
	        Throwable rootCause = e.getRootCause();
	        if (rootCause instanceof java.sql.SQLException) {
	            java.sql.SQLException sqlEx = (java.sql.SQLException) rootCause;
	            if ("45000".equals(sqlEx.getSQLState())) {
	                throw new RuntimeException(sqlEx.getMessage());
	            }
	        }
	        throw e;
	    }
	}
	public void addPromotion(promotion promotion) {
        String sql = "INSERT INTO promotion (Code, DiscountPercentage, ExpirationDate, CourseID, Status, CreateAt, UsageLimit, UsageCount) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                promotion.getCode(),
                promotion.getDiscountPercentage(),
                promotion.getExpirationDate(),
                promotion.getCourseID(),
                promotion.getStatus(),
                promotion.getCreateAt(),
                promotion.getUsageLimit(),
                promotion.getUsageCount());
    }
	
	public void updatePromotion(promotion promotion) {
        String sql = "UPDATE promotion SET Code = ?, DiscountPercentage = ?, ExpirationDate = ?, CourseID = ?, " +
                     "Status = ?, UsageLimit = ?, UsageCount = ? WHERE PromotionID = ?";
        jdbcTemplate.update(sql,
                promotion.getCode(),
                promotion.getDiscountPercentage(),
                promotion.getExpirationDate(),
                promotion.getCourseID(),
                promotion.getStatus(),
                promotion.getUsageLimit(),
                promotion.getUsageCount(),
                promotion.getPromotionID());
    }

    public List<promotion> findPromotionsPaginated(int page, int size, String search, String status) {
        StringBuilder sql = new StringBuilder(
                "SELECT p.*, c.Title as CourseTitle " +
                        "FROM promotion p " +
                        "LEFT JOIN course c ON p.CourseID = c.CourseID "
        );

        List<Object> params = new ArrayList<>();
        boolean hasWhereClause = false;

        // Thêm điều kiện tìm kiếm theo từ khóa (mã khuyến mãi hoặc tiêu đề khóa học)
        if (search != null && !search.trim().isEmpty()) {
            sql.append(hasWhereClause ? " AND " : " WHERE ");
            sql.append("(LOWER(p.Code) LIKE ? OR LOWER(c.Title) LIKE ?)");
            params.add("%" + search.toLowerCase() + "%");
            params.add("%" + search.toLowerCase() + "%");
            hasWhereClause = true;
        }

        // Thêm điều kiện lọc theo trạng thái
        if (status != null && !status.equals("all")) {
            sql.append(hasWhereClause ? " AND " : " WHERE ");
            sql.append("p.Status = ?");
            params.add(status);
            hasWhereClause = true;
        }

        // Thêm phân trang
        sql.append(" ORDER BY p.CreateAt DESC LIMIT ? OFFSET ?");
        params.add(size);
        params.add(page * size);

        try {
            return jdbcTemplate.query(sql.toString(), params.toArray(), this::mapRowToPromotion);
        } catch (Exception e) {
            throw new RuntimeException("Error fetching paginated promotions: " + e.getMessage());
        }
    }

    public long countPromotions(String search, String status) {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) " +
                        "FROM promotion p " +
                        "LEFT JOIN course c ON p.CourseID = c.CourseID "
        );

        List<Object> params = new ArrayList<>();
        boolean hasWhereClause = false;

        if (search != null && !search.trim().isEmpty()) {
            sql.append(hasWhereClause ? " AND " : " WHERE ");
            sql.append("(LOWER(p.Code) LIKE ? OR LOWER(c.Title) LIKE ?)");
            params.add("%" + search.toLowerCase() + "%");
            params.add("%" + search.toLowerCase() + "%");
            hasWhereClause = true;
        }

        if (status != null && !status.equals("all")) {
            sql.append(hasWhereClause ? " AND " : " WHERE ");
            sql.append("p.Status = ?");
            params.add(status);
        }

        try {
            Long count = jdbcTemplate.queryForObject(sql.toString(), params.toArray(), Long.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            throw new RuntimeException("Error counting promotions: " + e.getMessage());
        }
    }
}
