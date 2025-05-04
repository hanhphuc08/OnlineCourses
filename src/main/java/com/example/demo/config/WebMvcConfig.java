package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Map các URL đến các view tương ứng
        // registry.addViewController("/").setViewName("decorators/index");
        registry.addViewController("/home").setViewName("decorators/index");
        registry.addViewController("/login").setViewName("commons/login");
        registry.addViewController("/profile").setViewName("commons/profile");
        registry.addViewController("/register").setViewName("commons/register");
        registry.addViewController("/error").setViewName("error");
        registry.addViewController("/introduce").setViewName("commons/introduce");
        registry.addViewController("/schedule").setViewName("commons/schedule");
        registry.addViewController("/forgot-password").setViewName("commons/forgotpassword");
        registry.addViewController("/reset-password").setViewName("commons/resetpassword");
        registry.addViewController("/course/detail/**").setViewName("category/productdetail");
        registry.addViewController("/staff/addProducts").setViewName("staff/addProducts");
        registry.addViewController("/staff/addStaffs").setViewName("staff/addStaffs");
        registry.addViewController("/staff/categories").setViewName("staff/categories");
        registry.addViewController("/staff/mngcustomer").setViewName("staff/customer");
        registry.addViewController("/staff/dashboard").setViewName("staff/dashboard");
        registry.addViewController("/staff/orderDetail").setViewName("staff/orderDetail");
        registry.addViewController("/staff/ordersList").setViewName("staff/ordersList");
        registry.addViewController("/staff/productsList").setViewName("staff/productsList");
        registry.addViewController("/staff/staffsList").setViewName("staff/staffsList");
    }
} 