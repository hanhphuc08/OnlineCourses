package com.example.demo.controller;

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
	
	@GetMapping("/category")
	public String allCategories(Model model) {
		
		List<course> courses = categoryService.getAllCourses();
		
		List<categoryType> categoryTypes = categoryService.getAllWithCategoriesAndCourses();
		
        model.addAttribute("courses", courses);
        model.addAttribute("categoryTypes", categoryTypes);
        model.addAttribute("selectedTypeId", 0);
        model.addAttribute("selectedCategoryId", 0);

        return "category/allproduct";
	}
	
	
	// Hiển thị courses thuộc categoryType
	
    @GetMapping("/category/type/{id}")
    @ResponseBody
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

        logger.info("Courses thuộc CategoryType ID={}:", id);
        logger.info("CategoryType: ID={}, Name={}, Description={}, CreateDate={}, UpdateDate={}",
                type.getCategoryTypeID(),
                type.getCategoryTypeName(),
                type.getDescription(),
                type.getCreateDate(),
                type.getUpdateDate());

        if (categories.isEmpty()) {
            logger.info("  Không có categories nào thuộc CategoryType ID={}", id);
        } else {
            for (category cat : categories) {
                logger.info("  Category: ID={}, Name={}, Description={}, CreateDate={}, UpdateDate={}",
                        cat.getCategoryID(),
                        cat.getCategoryName(),
                        cat.getDescription(),
                        cat.getCreateDate(),
                        cat.getUpdateDate());

                List<course> catCourses = cat.getCourses();
                if (catCourses.isEmpty()) {
                    logger.info("    Không có courses nào thuộc Category ID={}", cat.getCategoryID());
                } else {
                    for (course c : catCourses) {
                        logger.info("    Course: ID={}, Title={}, Description={}, Prices={}, Quantity={}, Status={}, Duration={}, CreateAt={}, Image={}",
                                c.getCourseID(),
                                c.getTitle(),
                                c.getDescription(),
                                c.getPrices(),
                                c.getQuantity(),
                                c.getStatus(),
                                c.getDuration(),
                                c.getCreateAt(),
                                c.getImage());
                    }
                }
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
	@ResponseBody
	public String coursesByCategory(@PathVariable int id, Model model) {
		category cat = categoryService.getCategoryByIdWithCourses(id);
		List<categoryType> categoryTypes = categoryService.getAllWithCategoriesAndCourses();

		List<course> courses = cat.getCourses();

		logger.info("Courses thuộc Category ID={}:", id);
		logger.info("Category: ID={}, Name={}, Description={}, CreateDate={}, UpdateDate={}", cat.getCategoryID(),
				cat.getCategoryName(), cat.getDescription(), cat.getCreateDate(), cat.getUpdateDate());

		if (courses.isEmpty()) {
			logger.info("  Không có courses nào thuộc Category ID={}", id);
		} else {
			for (course c : courses) {
				logger.info(
						"  Course: ID={}, Title={}, Description={}, Prices={}, Quantity={}, Status={}, Duration={}, CreateAt={}, Image={}",
						c.getCourseID(), c.getTitle(), c.getDescription(), c.getPrices(), c.getQuantity(),
						c.getStatus(), c.getDuration(), c.getCreateAt(), c.getImage());
			}
		}

		model.addAttribute("courses", courses);
		model.addAttribute("categoryTypes", categoryTypes);
		model.addAttribute("selectedTypeId", 0);
		model.addAttribute("selectedCategoryId", id);

		return "category/allproduct";
	}
	
	
}
