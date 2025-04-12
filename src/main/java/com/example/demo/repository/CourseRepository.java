package com.example.demo.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.demo.model.category;
import com.example.demo.model.course;
import com.example.demo.model.courseStatus;

@Repository
public class CourseRepository {
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	private course mapRowToCourse(ResultSet rs, int rowNum) throws SQLException{
		course c = new course();
        c.setCourseID(rs.getInt("CourseID"));
        c.setTitle(rs.getString("Title"));
        c.setDescription(rs.getString("Description"));
        c.setPrices(rs.getBigDecimal("Prices"));
        c.setQuantity(rs.getInt("Quantity"));
        c.setStatus(courseStatus.valueOf(rs.getString("Status")));
        c.setDuration(rs.getObject("Duration", LocalDateTime.class));
        c.setCreateAt(rs.getObject("CreateAt", LocalDateTime.class));
        c.setImage(rs.getString("Image"));
        
        
        category cat = null;
        if (rs.getInt("CategoryID") != 0) {
            cat = new category();
            cat.setCategoryID(rs.getInt("CategoryID"));
            cat.setCategoryName(rs.getString("CategoryName"));
            cat.setCategoryTypeID(rs.getInt("CategoryTypeID"));
            cat.setDescription(rs.getString("CategoryDescription"));
            cat.setCreateDate(rs.getObject("CategoryCreateDate", LocalDateTime.class));
            cat.setUpdateDate(rs.getObject("CategoryUpdateDate", LocalDateTime.class));
        }
        c.setCategory(cat);
        return c;
	}
	
	// Get all courses with their categories
	public List<course> findAllCourses() {
        String sql = "SELECT c.*, cat.CategoryID, cat.CategoryName, cat.CategoryTypeID, cat.Description AS CategoryDescription, cat.CreateDate AS CategoryCreateDate, cat.UpdateDate AS CategoryUpdateDate " +
                     "FROM course c " +
                     "LEFT JOIN category cat ON c.CategoryID = cat.CategoryID";
        try {
            return jdbcTemplate.query(sql, this::mapRowToCourse);
        } catch (Exception e) {
            throw new RuntimeException("Error fetching courses: " + e.getMessage());
        }
    }

    // Get courses by CategoryID with their categories
    public List<course> findCoursesByCategoryId(int categoryId) {
        String sql = "SELECT c.*, cat.CategoryID, cat.CategoryName, cat.CategoryTypeID, cat.Description AS CategoryDescription, cat.CreateDate AS CategoryCreateDate, cat.UpdateDate AS CategoryUpdateDate " +
                     "FROM course c " +
                     "LEFT JOIN category cat ON c.CategoryID = cat.CategoryID " +
                     "WHERE c.CategoryID = ?";
        try {
            return jdbcTemplate.query(sql, this::mapRowToCourse, categoryId);
        } catch (Exception e) {
            throw new RuntimeException("Error fetching courses for CategoryID " + categoryId + ": " + e.getMessage());
        }
    }

}
