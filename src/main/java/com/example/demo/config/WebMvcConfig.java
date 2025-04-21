package com.example.demo.config;

import com.example.demo.interceptor.AuthenticationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthenticationInterceptor authenticationInterceptor;

    public WebMvcConfig(AuthenticationInterceptor authenticationInterceptor) {
        this.authenticationInterceptor = authenticationInterceptor;
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Map các URL đến các view tương ứng
        registry.addViewController("/").setViewName("decorators/index");
        registry.addViewController("/home").setViewName("decorators/index");
        registry.addViewController("/login").setViewName("commons/login");
        registry.addViewController("/register").setViewName("commons/register");
        registry.addViewController("/cart").setViewName("commons/cart");
        registry.addViewController("/error").setViewName("error");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authenticationInterceptor)
                .addPathPatterns("/**")  // Áp dụng cho tất cả các request
                .excludePathPatterns(    // Loại trừ các đường dẫn không cần kiểm tra
                        "/api/auth/**",
                        "/login",
                        "/register",
                        "/assets/**",
                        "/vendor/**",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/error",
                        "/api/courses/**",
                        "/courses",
                        "/category",
                        "/category/**"
                          
                );
    }
} 