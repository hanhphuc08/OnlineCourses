package com.example.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.course;
import com.example.demo.service.CourseService;

@RestController
@RequestMapping("/api/courses")
public class CourseController {
	
	private static final Logger logger = LoggerFactory.getLogger(CourseController.class);
	
	@Autowired
    private CourseService courseService;
	
	@GetMapping("/{id}")
    public ResponseEntity<course> getCourseById(@PathVariable("id") int courseId) {
        try {
            course course = courseService.getCourseById(courseId);
            logger.info("Course details: {}", course);
            return ResponseEntity.ok(course);
        } catch (RuntimeException e) {
            logger.error("Error fetching course with ID {}: {}", courseId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
