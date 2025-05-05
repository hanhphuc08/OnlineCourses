package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.course;
import com.example.demo.repository.CourseRepository;

import java.util.List;

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

	public List<course> getAllCourses() {
		List<course> courses = courseRepository.findAllCourses();
		System.out.println("CourseService: Returning " + courses.size() + " courses");
		return courses;
	}

	public void updateCourse(course course) {
		courseRepository.updateCourse(course);
	}
}
