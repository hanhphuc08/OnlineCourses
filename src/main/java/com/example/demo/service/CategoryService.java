package com.example.demo.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.category;
import com.example.demo.model.categoryType;
import com.example.demo.model.course;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.CourseRepository;

@Service
public class CategoryService {
	@Autowired
	private CategoryRepository categoryRepository;
	
	@Autowired
	private CourseRepository courseRepository;
	
	
	public List<categoryType> getAllCategoryTypes(){
		return categoryRepository.findAllCategoryTypes();
	}
	public List<category> getAllCategories(){
		return categoryRepository.findAllCategories();
	}
	
	public List<course> getAllCourses() {
        return courseRepository.findAllCourses();
    }
	
	
	
	// Get all category types with their categories and courses
    public List<categoryType> getAllWithCategoriesAndCourses() {
        return categoryRepository.findAllWithCategoriesAndCourses();
    }

    // Get a category type by ID with its categories and courses
    public categoryType getCategoryTypeByIdWithCourses(int id) {
        return categoryRepository.findCategoryTypeByIdWithCourses(id);
    }

    // Get a category by ID with its courses
    public category getCategoryByIdWithCourses(int id) {
        return categoryRepository.findCategoryByIdWithCourses(id);
    }
    
	// search
	public List<course> searchCourses(String keyword) {
		if (keyword == null || keyword.trim().isEmpty()) {
			return new ArrayList<>();
		}
		return courseRepository.searchByKeyword(keyword);
	}

	// Get the latest courses
	public List<course> getLatestCourses(int limit) {
		return courseRepository.findLatestCourses(limit);
	}
	

}
