package com.example.demo.controller.owner;

import com.example.demo.model.category;
import com.example.demo.model.course;
import com.example.demo.model.courseStatus;
import com.example.demo.model.users;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.CourseRepository;
import com.example.demo.service.CourseService;
import com.example.demo.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/owner")
@PreAuthorize("hasRole('Owner')")
public class OwnerController {

    private static final Logger logger = LoggerFactory.getLogger(OwnerController.class);

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private CourseService courseService;

    @GetMapping("/home")
    public String ownerHome() {
        return "owner/home";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        return "owner/dashboard";
    }

    @GetMapping("/users")
    public String userManagement(Model model) {
        return "owner/users";
    }

    @GetMapping("/productsList")
    public String productsList(Model model) {
        List<course> courseList = courseService.getAllCourses();
        model.addAttribute("courses", courseList);
        model.addAttribute("hasCourses", !courseList.isEmpty());
        return "owner/productsList";
    }

    @GetMapping("/edit")
    public String editCourseForm(@RequestParam("id") int id, Model model) {
        course course = courseService.getCourseById(id);
        List<category> categories = categoryRepository.findAllCategories();
        model.addAttribute("course", course);
        model.addAttribute("categories", categories);
        return "owner/addProducts";
    }

    @GetMapping("/customer")
    public String getCustomers(Model model) {
        List<users> customers = userService.findAllCustomers();
        model.addAttribute("customers", customers);
        return "owner/mngcustomer";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        return "owner/settings";
    }

    @GetMapping("/categories")
    public ResponseEntity<List<category>> getAllCategories() {
        try {
            List<category> categories = categoryRepository.findAllCategories();
            logger.info("Fetched {} categories", categories.size());
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            logger.error("Error fetching categories: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/course/edit")
    public ResponseEntity<String> updateCourse(@RequestBody CourseUpdateDTO courseDTO) {
        try {
            logger.info("Received course update request: {}", courseDTO);

            if (courseDTO.getCourseID() <= 0) {
                logger.error("Invalid Course ID: {}", courseDTO.getCourseID());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid Course ID");
            }
            if (courseDTO.getTitle() == null || courseDTO.getTitle().trim().isEmpty()) {
                logger.error("Title is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Title is required");
            }
            if (courseDTO.getPrices() == null || courseDTO.getPrices().doubleValue() < 0) {
                logger.error("Price must be non-negative: {}", courseDTO.getPrices());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Price must be non-negative");
            }
            if (courseDTO.getQuantity() < 0) {
                logger.error("Quantity must be non-negative: {}", courseDTO.getQuantity());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Quantity must be non-negative");
            }

            course course = new course();
            course.setCourseID(courseDTO.getCourseID());
            course.setTitle(courseDTO.getTitle());
            course.setDescription(courseDTO.getDescription());
            course.setPrices(courseDTO.getPrices());
            course.setStatus(courseStatus.valueOf(courseDTO.getStatus()));
            course.setImage(courseDTO.getImage());
            course.setQuantity(courseDTO.getQuantity());
            course.setDuration(null);

            if (courseDTO.getCategory() != null && courseDTO.getCategory().getCategoryID() > 0) {
                category cat = new category();
                cat.setCategoryID(courseDTO.getCategory().getCategoryID());
                course.setCategory(cat);
            } else {
                course.setCategory(null);
                logger.warn("Category is null or invalid, setting course category to null");
            }

            courseService.updateCourse(course);
            logger.info("Course updated successfully: {}", courseDTO.getCourseID());
            return ResponseEntity.ok("Course updated successfully");
        } catch (Exception e) {
            logger.error("Error updating course: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating course: " + e.getMessage());
        }
    }
}

class CourseUpdateDTO {
    private int courseID;
    private String title;
    private String description;
    private BigDecimal prices;
    private String status;
    private String image;
    private int quantity;
    private CategoryDTO category;

    public int getCourseID() { return courseID; }
    public void setCourseID(int courseID) { this.courseID = courseID; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrices() { return prices; }
    public void setPrices(BigDecimal prices) { this.prices = prices; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public CategoryDTO getCategory() { return category; }
    public void setCategory(CategoryDTO category) { this.category = category; }

    @Override
    public String toString() {
        return "CourseUpdateDTO{" +
                "courseID=" + courseID +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", prices=" + prices +
                ", status='" + status + '\'' +
                ", image='" + image + '\'' +
                ", quantity=" + quantity +
                ", category=" + category +
                '}';
    }
}

class CategoryDTO {
    private int categoryID;

    public int getCategoryID() { return categoryID; }
    public void setCategoryID(int categoryID) { this.categoryID = categoryID; }

    @Override
    public String toString() {
        return "CategoryDTO{categoryID=" + categoryID + '}';
    }
}