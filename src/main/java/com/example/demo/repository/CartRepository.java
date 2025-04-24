package com.example.demo.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.demo.model.cart;

@Repository
public class CartRepository {
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	private cart mapRowToCart(ResultSet rs, int rowNum) throws SQLException {
        cart cart = new cart();
        cart.setCartID(rs.getInt("cartID"));
        cart.setUserID(rs.getInt("userID"));
        cart.setCourseID(rs.getInt("courseID"));
        cart.setQuantity(rs.getInt("quantity"));
        cart.setCreatedAt(rs.getTimestamp("createdAt").toLocalDateTime());
        return cart;
    }
	
	public void addToCart(int userID, int courseID, int quantity) {
		String checkSql = "SELECT COUNT(*) FROM cart WHERE userID = ? AND courseID = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, new Object[]{userID, courseID}, Integer.class);

        if (count != null && count > 0) {
            throw new RuntimeException("Đã có khóa học này trong giỏ hàng");
        }

        String insertSql = "INSERT INTO cart (userID, courseID, quantity, createDate) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(insertSql, userID, courseID, quantity, LocalDateTime.now());
		
	}
	public List<cart> findByUserId(int userId) {
        String sql = "SELECT * FROM cart WHERE userID = ?";
        return jdbcTemplate.query(sql, this::mapRowToCart, userId);
    }

}
