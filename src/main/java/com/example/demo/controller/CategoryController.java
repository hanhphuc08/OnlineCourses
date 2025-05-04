package com.example.demo.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.demo.model.category;
import com.example.demo.model.categoryType;
import com.example.demo.model.course;
import com.example.demo.service.CategoryService;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
public class CategoryController {
	private static final Logger logger = LoggerFactory.getLogger(CategoryController.class);
	
	@Autowired
	private CategoryService categoryService;
	
	@GetMapping("/")
    public String index(Model model) {
        try {
            List<course> latestCourses = categoryService.getLatestCourses(4);
            logger.info("Latest courses: {}", latestCourses); // Thêm log để kiểm tra dữ liệu
            model.addAttribute("courses", latestCourses);
            model.addAttribute("isHomePage", true);
            return "decorators/index";
        } catch (Exception e) {
            logger.error("Error loading latest courses: ", e);
            model.addAttribute("error", "Failed to load courses: " + e.getMessage());
            model.addAttribute("isHomePage", true);
            return "error";
        }
    }

	// search
	@GetMapping("/search")
	public String searchCourses(@RequestParam("keyword") String keyword) {
		logger.info("Search request received with keyword: {}", keyword);
		try {
			String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8.toString());
			return "redirect:/category?keyword=" + encodedKeyword;
		} catch (UnsupportedEncodingException e) {
			logger.error("Error encoding search keyword: {}", e.getMessage());
			return "redirect:/category?error=encoding";
		}
	}

	@GetMapping("/category")
	public String allCategories(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
		
		List<course> courses = categoryService.getAllCourses();
		List<categoryType> categoryTypes = categoryService.getAllWithCategoriesAndCourses();
		if (keyword != null && !keyword.isEmpty()) {
	        courses = categoryService.searchCourses(keyword);
	    } else {
	        courses = categoryService.getAllCourses();
	    }  
        model.addAttribute("courses", courses);
        model.addAttribute("categoryTypes", categoryTypes);
        model.addAttribute("selectedTypeId", 0);
        model.addAttribute("selectedCategoryId", 0);
        model.addAttribute("keyword", keyword); 

        return "category/allproduct";
	}
	
	// Hiển thị courses thuộc categoryType
    @GetMapping("/category/type/{id}")
    public String coursesByCategoryType(@PathVariable int id, Model model) {
        categoryType type = categoryService.getCategoryTypeByIdWithCourses(id);
        List<categoryType> categoryTypes = categoryService.getAllWithCategoriesAndCourses();

        List<course> courses = new ArrayList<>();
        List<category> categories = type.getCategories();
        
        for (category cat : categories) {
            if (cat.getCourses() != null) {
                courses.addAll(cat.getCourses());
            }
        }

        model.addAttribute("courses", courses);
        model.addAttribute("categoryTypes", categoryTypes);
        model.addAttribute("selectedTypeId", id); 
        model.addAttribute("selectedCategoryId", 0); 

        return "category/allproduct";
    }
	
	
	// Hiển thị courses thuộc category
	@GetMapping("/category/{id}")
	public String coursesByCategory(@PathVariable int id, Model model) {
		category cat = categoryService.getCategoryByIdWithCourses(id);
		List<categoryType> categoryTypes = categoryService.getAllWithCategoriesAndCourses();

		List<course> courses = cat.getCourses();
		model.addAttribute("courses", courses);
		model.addAttribute("categoryTypes", categoryTypes);
		model.addAttribute("selectedTypeId", 0);
		model.addAttribute("selectedCategoryId", id);

		return "category/allproduct";
	}
}
