package com.example.demo.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.demo.model.category;
import com.example.demo.model.categoryType;
import com.example.demo.model.course;
import com.example.demo.model.courseStatus;
import com.example.demo.model.learningPath;
import com.example.demo.service.UserService;

@Repository
public class CourseRepository {
	private static final Logger logger = LoggerFactory.getLogger(UserService.class);
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	private course mapRowToCourse(ResultSet rs, int rowNum) throws SQLException{
		course c = new course();
        c.setCourseID(rs.getInt("CourseID"));
        c.setTitle(rs.getString("Title"));
        c.setDescription(rs.getString("Description"));
        c.setPrices(rs.getBigDecimal("Prices"));
        c.setQuantity(rs.getInt("Quantity"));
        String status = rs.getString("Status");
        try {
            c.setStatus(courseStatus.valueOf(status != null ? status.trim().toUpperCase() : "INACTIVE"));
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid status value: " + status + ", defaulting to INACTIVE");
            c.setStatus(courseStatus.INACTIVE);
        }
        c.setDuration(rs.getObject("Duration", LocalDateTime.class));
        c.setCreateAt(rs.getObject("CreateAt", LocalDateTime.class));
        String image = rs.getString("Image");
        
        c.setImage(image);
        
        
        category cat = null;
        if (rs.getInt("CategoryID") != 0) {
            cat = new category();
            cat.setCategoryID(rs.getInt("CategoryID"));
            cat.setCategoryName(rs.getString("CategoryName"));
            cat.setDescription(rs.getString("Description"));
            cat.setCreateDate(rs.getObject("CreateDate", LocalDateTime.class));
            cat.setUpdateDate(rs.getObject("UpdateDate", LocalDateTime.class));
        }
        c.setCategory(cat);
        return c;
	}
	
	private category mapRowToCategory(ResultSet rs) throws SQLException {
        category cat = new category();
        cat.setCategoryID(rs.getInt("CategoryID"));
        cat.setCategoryName(rs.getString("CategoryName"));
        cat.setDescription(rs.getString("description"));
        cat.setCreateDate(rs.getObject("CreateDate", LocalDateTime.class));
        cat.setUpdateDate(rs.getObject("UpdateDate", LocalDateTime.class));
        return cat;
    }
	private learningPath mapRowToLearningPath(ResultSet rs, int rowNum) throws SQLException {
        learningPath lp = new learningPath();
        lp.setLearningPathID(rs.getInt("learningPathID"));
        lp.setStepNumber(rs.getInt("stepNumber"));
        lp.setTitle(rs.getString("Title"));
        lp.setCreateAt(rs.getObject("CreateAt", LocalDateTime.class));

        course courses = new course();
        courses.setCourseID(rs.getInt("courseID"));
        lp.setCourse(courses);

        return lp;
    }
	// Get all courses with their categories
    public List<course> findAllCourses() {
        String sql = "SELECT c.*, cat.CategoryID, cat.CategoryName, cat.Description, cat.CreateDate, cat.UpdateDate " +
                "FROM course c " +
                "LEFT JOIN category cat ON c.CategoryID = cat.CategoryID " +
                "WHERE c.Status = 'ACTIVE'";
        try {
            List<course> courses = jdbcTemplate.query(sql, this::mapRowToCourse);
            System.out.println("CourseRepository: Fetched " + courses.size() + " courses");
            return courses;
        } catch (Exception e) {
            System.err.println("CourseRepository: Error fetching courses: " + e.getMessage());
            throw new RuntimeException("Error fetching courses: " + e.getMessage());
        }
    }
    
    public List<course> findAllCourses1() {
        String sql = "SELECT c.*, cat.CategoryID, cat.CategoryName, cat.Description, cat.CreateDate, cat.UpdateDate " +
                "FROM course c " +
                "LEFT JOIN category cat ON c.CategoryID = cat.CategoryID ";
        try {
            List<course> courses = jdbcTemplate.query(sql, this::mapRowToCourse);
            System.out.println("CourseRepository: Fetched " + courses.size() + " courses");
            return courses;
        } catch (Exception e) {
            System.err.println("CourseRepository: Error fetching courses: " + e.getMessage());
            throw new RuntimeException("Error fetching courses: " + e.getMessage());
        }
    }

    // Get courses by CategoryID with their categories
    public List<course> findCoursesByCategoryId(int categoryId) {
        String sql = "SELECT c.*, cat.CategoryID, cat.CategoryName, cat.Description, cat.CreateDate, cat.UpdateDate " +
                "FROM course c " +
                "LEFT JOIN category cat ON c.CategoryID = cat.CategoryID " +
                "WHERE c.CategoryID = ? AND c.Status = 'ACTIVE'";
        try {
            return jdbcTemplate.query(sql, this::mapRowToCourse, categoryId);
        } catch (Exception e) {
            throw new RuntimeException("Error fetching courses for CategoryID " + categoryId + ": " + e.getMessage());
        }
    }
    
    private List<learningPath> findLearningPathsByCourseId(int courseID) {
        String sql = "SELECT lp.*, c.CourseID " +
                     "FROM learningPath lp " +
                     "JOIN course c ON lp.courseID = c.CourseID " +
                     "WHERE lp.courseID = ? ORDER BY lp.stepNumber ASC";
        try {
            return jdbcTemplate.query(sql, new Object[]{courseID}, this::mapRowToLearningPath);
        } catch (Exception e) {
            throw new RuntimeException("Error fetching learning paths for CourseID " + courseID + ": " + e.getMessage());
        }
    }
    
    public course findById(int courseID) {
        String sql = "SELECT c.*, cat.* " +
                "FROM course c " +
                "LEFT JOIN category cat ON c.CategoryID = cat.CategoryID " +
                "WHERE c.CourseID = ?";

        try {
            course courses = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                course co = mapRowToCourse(rs, rowNum);
                co.setCategory(mapRowToCategory(rs));
                return co;
            }, courseID);

            if (courses != null) {
                List<learningPath> learningPaths = findLearningPathsByCourseId(courseID);
                courses.setLearningPaths(learningPaths);
            }

            return courses;
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Error fetching course with ID " + courseID + ": " + e.getMessage());
        }
    }
    
    public void decrementQuantity(int courseId) {
    	
        String sql = "UPDATE course SET Quantity = Quantity - 1 WHERE CourseID = ? AND Quantity > 0";
        int rowsAffected = jdbcTemplate.update(sql, courseId);
        if (rowsAffected == 0) {
        	
            throw new RuntimeException("Không thể giảm số lượng khóa học: Khóa học không tồn tại hoặc đã hết!");
        }
    }

	// search by keyword, split and search in title/description
		public List<course> searchByKeyword(String keyword) {
		    // Tách từ khóa thành các từ riêng lẻ và chuẩn bị mẫu tìm kiếm
		    String[] keywords = keyword.trim().toLowerCase().split("\\s+");
		    List<String> params = new ArrayList<>();
		    StringBuilder whereClause = new StringBuilder();

		    // Xây dựng điều kiện WHERE động
		    for (int i = 0; i < keywords.length; i++) {
		        if (i > 0) {
		            whereClause.append(" AND ");
		        }
		        whereClause.append("(LOWER(c.Title) LIKE ? OR LOWER(c.Description) LIKE ?)");
		        params.add("%" + keywords[i] + "%");
		        params.add("%" + keywords[i] + "%");
		    }
		    String sql = "SELECT c.*, cat.CategoryID, cat.CategoryName, cat.Description, " +
	                 "cat.CreateDate, cat.UpdateDate " +
	                 "FROM course c " +
	                 "LEFT JOIN category cat ON c.CategoryID = cat.CategoryID " +
	                 "WHERE " + whereClause.toString() + " AND c.Status = 'ACTIVE' " +
	                 "ORDER BY c.CreateAt DESC";

		    try {
		        return jdbcTemplate.query(sql, params.toArray(), this::mapRowToCourse);
		    } catch (Exception e) {
		        throw new RuntimeException("Error searching courses with keyword '" + keyword + "': " + e.getMessage());
		    }
		}
		
		// Get latest courses, sorted by CreateAt descending, limited to a specific number
		public List<course> findLatestCourses(int limit) {
		    String sql = "SELECT c.*, cat.CategoryID, cat.CategoryName, cat.Description, cat.CreateDate, cat.UpdateDate " +
	                 "FROM course c " +
	                 "LEFT JOIN category cat ON c.CategoryID = cat.CategoryID " +
	                 "WHERE c.Status = 'ACTIVE' " +
	                 "ORDER BY c.CreateAt DESC " +
	                 "LIMIT ?";
		    try {
		        return jdbcTemplate.query(sql, new Object[]{limit}, this::mapRowToCourse);
		    } catch (Exception e) {
		        throw new RuntimeException("Error fetching latest courses: " + e.getMessage());
		    }
		}
		
		 // get courses CategoryTypeId
	    public List<course> findCoursesByCategoryTypeId(int categoryTypeId) {
	        String sql = "SELECT c.*, cat.CategoryID, cat.CategoryName, cat.Description, " +
	                 "cat.CreateDate, cat.UpdateDate " +
	                 "FROM course c " +
	                 "LEFT JOIN category cat ON c.CategoryID = cat.CategoryID " +
	                 "LEFT JOIN category_type ct ON cat.CategoryTypeID = ct.CategoryTypeID " +
	                 "WHERE ct.CategoryTypeID = ? AND c.Status = 'ACTIVE'";
	        
	        try {
	            return jdbcTemplate.query(sql, new Object[]{categoryTypeId}, this::mapRowToCourse);
	        } catch (Exception e) {
	            throw new RuntimeException("Error fetching courses for CategoryTypeID " + 
	                                     categoryTypeId + ": " + e.getMessage());
	        }
	    }

    public void updateCourse(course course) {
        String sql = "UPDATE course SET Title = ?, Description = ?, Prices = ?, Status = ?, Image = ?, Duration = ?, CategoryID = ?, Quantity = ? WHERE CourseID = ?";
        try {
            jdbcTemplate.update(sql,
                    course.getTitle(),
                    course.getDescription(),
                    course.getPrices(),
                    course.getStatus().toString(),
                    course.getImage(),
                    course.getDuration(),
                    course.getCategory() != null ? course.getCategory().getCategoryID() : null,
                    course.getQuantity(),
                    course.getCourseID());
        } catch (Exception e) {
            throw new RuntimeException("Error updating course with ID " + course.getCourseID() + ": " + e.getMessage());
        }
    }

    public void addCourse(course course) {
        String sql = "INSERT INTO course (Title, Description, Prices, Status, Image, Duration, CategoryID, Quantity, CreateAt) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            jdbcTemplate.update(sql,
                    course.getTitle(),
                    course.getDescription(),
                    course.getPrices(),
                    course.getStatus().toString(),
                    course.getImage(),
                    course.getDuration(),
                    course.getCategory() != null ? course.getCategory().getCategoryID() : null,
                    course.getQuantity(),
                    course.getCreateAt());
        } catch (Exception e) {
            throw new RuntimeException("Error adding course: " + e.getMessage());
        }
    }

    
    public long countAllCourses() {
        String sql = "SELECT COUNT(*) FROM course";
        try {
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            logger.info("Đếm tổng số khóa học: {}", count);
            return count != null ? count : 0;
        } catch (Exception e) {
            logger.error("Lỗi khi đếm tổng số khóa học: {}", e.getMessage());
            throw new RuntimeException("Lỗi khi đếm tổng số khóa học: " + e.getMessage());
        }
    }

    public long countActiveCourses() {
        String sql = "SELECT COUNT(*) FROM course WHERE Status = 'ACTIVE'";
        try {
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            logger.info("Đếm số khóa học đang hoạt động: {}", count);
            return count != null ? count : 0;
        } catch (Exception e) {
            logger.error("Lỗi khi đếm số khóa học đang hoạt động: {}", e.getMessage());
            throw new RuntimeException("Lỗi khi đếm số khóa học đang hoạt động: " + e.getMessage());
        }
    }
    
    public List<Object[]> findTopCoursesByEnrollments(int limit) {
        String sql = "SELECT c.CourseID, c.Title, COUNT(od.CourseID) as enrollmentCount " +
                     "FROM course c " +
                     "LEFT JOIN orderDetail od ON c.CourseID = od.CourseID " +
                     "GROUP BY c.CourseID, c.Title " +
                     "ORDER BY enrollmentCount DESC " +
                     "LIMIT ?";
        try {
            return jdbcTemplate.query(sql, new Object[]{limit}, (rs, rowNum) -> new Object[]{
                rs.getInt("CourseID"),
                rs.getString("Title"),
                rs.getLong("enrollmentCount")
            });
        } catch (Exception e) {
            logger.error("Lỗi khi lấy top khóa học bán chạy: {}", e.getMessage());
            throw new RuntimeException("Lỗi khi lấy top khóa học bán chạy: " + e.getMessage());
        }
    }
    
    
    
}
