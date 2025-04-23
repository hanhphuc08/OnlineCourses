package com.example.demo.interceptor;

import com.example.demo.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import io.jsonwebtoken.Claims;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public AuthenticationInterceptor(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Lấy đường dẫn hiện tại
        String path = request.getRequestURI();
        System.out.println("Processing request to: " + path);
        
        // Bỏ qua các đường dẫn công khai
        if (isPublicPath(path)) {
            System.out.println("Skipping authentication for public path: " + path);
            return true;
        }
        
        // Lấy token từ header
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        // Nếu không có token trong header, kiểm tra trong URL parameter
        if (token == null) {
            token = request.getParameter("token");
        }

        // Kiểm tra token
        if (token != null) {
            try {
                Claims claims = jwtUtil.extractAllClaims(token);
                
                // Kiểm tra token có hết hạn không
                if (claims.getExpiration().before(new java.util.Date())) {
                    response.sendRedirect("/login");
                    return false;
                }

                return true;
                
            } catch (Exception e) {
                System.out.println("Error processing token: " + e.getMessage());
                response.sendRedirect("/login");
                return false;
            }
        }

        response.sendRedirect("/login");
        return false;
    }
    
    // Kiểm tra xem đường dẫn có phải là đường dẫn công khai không
    private boolean isPublicPath(String path) {
        return path.equals("/") || 
               path.equals("/home") || 
               path.equals("/login") || 
               path.equals("/register") || 
               path.equals("/introduce") ||
               path.equals("/schedule") ||
               path.equals("/forgot-password") ||
               path.equals("/category") ||
               path.startsWith("/assets/") || 
               path.startsWith("/vendor/") || 
               path.startsWith("/css/") || 
               path.startsWith("/js/") || 
               path.startsWith("/images/") || 
               path.startsWith("/api/auth/") || 
               path.startsWith("/category/") ||
               path.startsWith("/courses/") ||
               path.startsWith("/course/detail/") ||
               path.equals("/error");
    }
} 