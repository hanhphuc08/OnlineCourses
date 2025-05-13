package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.course;
import com.example.demo.repository.CourseRepository;

import java.util.List;

@Service
public class CourseService {


	private static final Logger logger = LoggerFactory.getLogger(CourseService.class);



	@Autowired
	private CourseRepository courseRepository;

	public course getCourseById(int courseId) {
		course courses = courseRepository.findById(courseId);
		if (courses == null) {
			throw new RuntimeException("Course not found with ID: " + courseId);
		}
		logger.info("Fetched course ID: {}, Status: {}", courseId, courses.getStatus());
		return courses;
	}

	public List<course> getAllCourses() {
		List<course> courses = courseRepository.findAllCourses();
		return courses;
	}
	
	
	public List<course> getAllCourses1() {
		List<course> courses = courseRepository.findAllCourses1();
		return courses;
	}
	

	public void updateCourse(course course) {
		if (course.getCourseID() <= 0) {
			throw new IllegalArgumentException("Invalid Course ID");
		}
		if (course.getTitle() == null || course.getTitle().trim().isEmpty()) {
			throw new IllegalArgumentException("Title is required");
		}
		if (course.getPrices() == null || course.getPrices().doubleValue() < 0) {
			throw new IllegalArgumentException("Price must be non-negative");
		}
		if (course.getQuantity() < 0) {
			throw new IllegalArgumentException("Quantity must be non-negative");
		}
		logger.info("Updating course ID: {}, Status: {}, Quantity: {}",
				course.getCourseID(), course.getStatus(), course.getQuantity());
		courseRepository.updateCourse(course);
	}

	public void addCourse(course course) {
		if (course.getTitle() == null || course.getTitle().trim().isEmpty()) {
			throw new IllegalArgumentException("Title is required");
		}
		if (course.getPrices() == null || course.getPrices().doubleValue() < 0) {
			throw new IllegalArgumentException("Price must be non-negative");
		}
		if (course.getQuantity() < 0) {
			throw new IllegalArgumentException("Quantity must be non-negative");
		}
		logger.info("Adding new course: title={}, status={}, quantity={}",
				course.getTitle(), course.getStatus(), course.getQuantity());
		courseRepository.addCourse(course);
	}


	
	public long countAllCourses() {
        logger.info("Đếm tổng số khóa học");
        return courseRepository.countAllCourses();
    }

    public long countActiveCourses() {
        logger.info("Đếm số khóa học đang hoạt động");
        return courseRepository.countActiveCourses();
    }
    public List<Object[]> findTopCoursesByEnrollments(int limit) {
        logger.info("Lấy top {} khóa học bán chạy nhất", limit);
        return courseRepository.findTopCoursesByEnrollments(limit);
    }
 
	
}

