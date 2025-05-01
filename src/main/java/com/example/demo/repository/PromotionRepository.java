package com.example.demo.repository;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.demo.model.promotion;

@Repository
public class PromotionRepository {
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	
	private promotion mapRowToPromotion(ResultSet rs, int rowNum) throws SQLException {
        promotion promotion = new promotion();
        promotion.setPromotionID(rs.getInt("PromotionID"));
        promotion.setCode(rs.getString("Code"));
        promotion.setDiscountPercentage(rs.getBigDecimal("DiscountPercentage"));
        promotion.setExpirationDate(rs.getTimestamp("ExpirationDate").toLocalDateTime());
        promotion.setCourseID(rs.getInt("CourseID"));
        promotion.setStatus(rs.getString("Status"));
        promotion.setCreateAt(rs.getTimestamp("CreateAt").toLocalDateTime());
        promotion.setUsageLimit(rs.getInt("UsageLimit"));
        promotion.setUsageCount(rs.getInt("UsageCount"));
        return promotion;
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

}
