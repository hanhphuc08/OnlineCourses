package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.course;
import com.example.demo.repository.CourseRepository;

@Service
public class CourseService {
	@Autowired
	public CourseRepository courseRepository;
	
	public course getCourseById(int courseId) {
		course courses = courseRepository.findById(courseId);
		if(courses == null) {
			throw new RuntimeException("Course not found with ID: " + courseId);
		}
		return courses;
	}
}
