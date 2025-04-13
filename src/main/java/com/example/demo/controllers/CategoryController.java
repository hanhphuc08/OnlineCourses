package com.example.demo.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CategoryController {
	@GetMapping("/category")
    public String product() {
        return "category/allproduct";
    }
	 @GetMapping("/product-detail")
	    public String productDetail() {
	        return "category/productdetail";
	    }
}
