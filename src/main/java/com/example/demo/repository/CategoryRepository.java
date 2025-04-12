package com.example.demo.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.demo.model.category;
import com.example.demo.model.categoryType;
import com.example.demo.model.course;

@Repository
public class CategoryRepository {
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private CourseRepository courseRepository;
	
	private categoryType mapRowToCategoryType(ResultSet rs, int rowNum) throws SQLException{
		
		categoryType type = new categoryType();
		type.setCategoryTypeID(rs.getInt("CategoryTypeID"));
		type.setCategoryTypeName(rs.getString("CategoryTypeName"));
		type.setDescription(rs.getString("Description"));
		type.setCreateDate(rs.getObject("CreateDate", LocalDateTime.class));
        type.setUpdateDate(rs.getObject("UpdateDate", LocalDateTime.class));
		return type;
	}
	
	private category mapRowToCategory(ResultSet rs, int rowNum) throws SQLException{
		category cate = new category();
		cate.setCategoryID(rs.getInt("CategoryID"));
        cate.setCategoryName(rs.getString("CategoryName"));
        cate.setCategoryTypeID(rs.getInt("CategoryTypeID"));
        cate.setDescription(rs.getString("Description"));
        cate.setCreateDate(rs.getObject("CreateDate", LocalDateTime.class));
        cate.setUpdateDate(rs.getObject("UpdateDate", LocalDateTime.class));
		
		return cate;
	}
	
	public List<categoryType> findAllCategoryTypes()
	{
		String sql = "SELECT * FROM categorytype";
        try {
            return jdbcTemplate.query(sql, this::mapRowToCategoryType);
        } catch (Exception e) {
            throw new RuntimeException("Error fetching category types: " + e.getMessage());
        }
	}
	
	public List<category> findAllCategories() {
		String sql = "SELECT * FROM category";
        try {
            return jdbcTemplate.query(sql, this::mapRowToCategory);
        } catch (Exception e) {
            throw new RuntimeException("Error fetching categories: " + e.getMessage());
        }
    }
	public List<categoryType> findAllWithCategoriesAndCourses() {

        List<categoryType> categoryTypes = findAllCategoryTypes();

        List<category> categories = findAllCategories();
        
        List<course> courses = courseRepository.findAllCourses();

        // Map để nhóm các category theo CategoryTypeID
        Map<Integer, List<category>> categoryMap = new HashMap<>();
        for (category cate : categories) {
            categoryMap.computeIfAbsent(cate.getCategoryTypeID(), k -> new ArrayList<>()).add(cate);
        }
        
        // Map để nhóm các course theo CategoryID
        Map<Integer, List<course>> courseMap = new HashMap<>();
        for (course c : courses) {
            if (c.getCategory() != null) { 
                courseMap.computeIfAbsent(c.getCategory().getCategoryID(), k -> new ArrayList<>()).add(c);
            }
        }
        
        // Gán danh sách categories và courses vào từng categoryType
        for (categoryType type : categoryTypes) {
            List<category> typeCategories = categoryMap.getOrDefault(type.getCategoryTypeID(), new ArrayList<>());
            for (category cate : typeCategories) {
                cate.setCourses(courseMap.getOrDefault(cate.getCategoryID(), new ArrayList<>()));
            }
            type.setCategories(typeCategories);
        }

        return categoryTypes;
    }
	
	// Get category type by ID with its categories and courses
    public categoryType findCategoryTypeByIdWithCourses(int id) {
        String sql = "SELECT * FROM categorytype WHERE CategoryTypeID = ?";
        try {
            categoryType type = jdbcTemplate.queryForObject(sql, this::mapRowToCategoryType, id);

            String categorySql = "SELECT * FROM category WHERE CategoryTypeID = ?";
            List<category> categories = jdbcTemplate.query(categorySql, this::mapRowToCategory, id);

            List<course> courses = courseRepository.findAllCourses();

            // Map để nhóm các course theo CategoryID
            Map<Integer, List<course>> courseMap = new HashMap<>();
            for (course c : courses) {
                courseMap.computeIfAbsent(c.getCategory().getCategoryID(), k -> new ArrayList<>()).add(c);
            }

            // Gán danh sách courses vào từng category
            for (category cat : categories) {
                cat.setCourses(courseMap.getOrDefault(cat.getCategoryID(), new ArrayList<>()));
            }

            type.setCategories(categories);
            return type;
        } catch (Exception e) {
            throw new RuntimeException("Error fetching category type with ID " + id + ": " + e.getMessage());
        }
    }

    // Get category by ID with its courses
    public category findCategoryByIdWithCourses(int id) {
        String sql = "SELECT * FROM category WHERE CategoryID = ?";
        try {
            category cat = jdbcTemplate.queryForObject(sql, this::mapRowToCategory, id);

            // Lấy danh sách courses cho category này
            List<course> courses = courseRepository.findCoursesByCategoryId(id);
            cat.setCourses(courses);

            return cat;
        } catch (Exception e) {
            throw new RuntimeException("Error fetching category with ID " + id + ": " + e.getMessage());
        }
    }
	
	
}
