package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Map các URL đến các view tương ứng
        registry.addViewController("/").setViewName("decorators/index");
        registry.addViewController("/home").setViewName("decorators/index");
        registry.addViewController("/login").setViewName("commons/login");
        registry.addViewController("/register").setViewName("commons/register");
        registry.addViewController("/error").setViewName("error");
        registry.addViewController("/introduce").setViewName("commons/introduce");
        registry.addViewController("/schedule").setViewName("commons/schedule");
        registry.addViewController("/forgot-password").setViewName("commons/forgotpassword");
        registry.addViewController("/course/detail/**").setViewName("category/productdetail");
    }
} 