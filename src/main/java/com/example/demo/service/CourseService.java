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
		courses.forEach(course -> logger.info("Course ID: {}, Status: {}", course.getCourseID(), course.getStatus()));
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
		logger.info("Updating course ID: {}, Status: {}", course.getCourseID(), course.getStatus());
		courseRepository.updateCourse(course);
	}
}