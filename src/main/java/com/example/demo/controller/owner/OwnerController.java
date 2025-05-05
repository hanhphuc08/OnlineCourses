package com.example.demo.controller.owner;

import com.example.demo.model.course;
import com.example.demo.model.users;
import com.example.demo.repository.CourseRepository;
import com.example.demo.service.CourseService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/owner")
@PreAuthorize("hasRole('Owner')")
public class OwnerController {

    @Autowired
    private CourseRepository courseRepository;

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
        // TODO: Add dashboard statistics
        return "owner/dashboard";
    }

    @GetMapping("/users")
    public String userManagement(Model model) {
        // TODO: Add user management logic
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
        model.addAttribute("course", course);
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
        // TODO: Add settings management
        return "owner/settings";
    }
}
