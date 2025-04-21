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
        
        // Danh sách các đường dẫn công khai không cần đăng nhập
        if (isPublicPath(path)) {
            System.out.println("Public path, allowing access: " + path);
            return true;
        }
        
        // Lấy token từ header
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            System.out.println("Token found in Authorization header");
        }
        
        // Nếu không có token trong header, kiểm tra trong URL parameter
        if (token == null) {
            token = request.getParameter("token");
            if (token != null) {
                System.out.println("Token found in URL parameter");
            }
        }
        
        // Nếu không có token trong header hoặc URL, kiểm tra trong cookie
        if (token == null) {
            jakarta.servlet.http.Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (jakarta.servlet.http.Cookie cookie : cookies) {
                    if (cookie.getName().equals("jwt_token")) {
                        token = cookie.getValue();
                        System.out.println("Token found in cookie");
                        break;
                    }
                }
            }
        }

        // Kiểm tra token
        if (token != null) {
            try {
                // Lấy claims từ token
                Claims claims = jwtUtil.extractAllClaims(token);
                System.out.println("Token claims: " + claims);
                
                // Kiểm tra token có hết hạn không
                if (claims.getExpiration().before(new java.util.Date())) {
                    System.out.println("Token expired, redirecting to login");
                    response.sendRedirect("/login");
                    return false;
                }

                // Lấy role từ claims
                String role = claims.get("role", String.class);
                System.out.println("User role from token: " + role);
                
                // Kiểm tra quyền truy cập dựa trên đường dẫn và role
                if (path.startsWith("/cart")) {
                    System.out.println("Accessing cart page");
                    if (role != null) {
                        System.out.println("Access granted to cart for role: " + role);
                        return true;
                    } else {
                        System.out.println("Access denied to cart - No role found");
                        response.sendRedirect("/login");
                        return false;
                    }
                }
                
                // Cho phép tất cả user đã đăng nhập truy cập các trang chung
                if (path.startsWith("/courses") || path.equals("/") || path.equals("/home")) {
                    System.out.println("Access granted to public path: " + path);
                    return true;
                }
                
                System.out.println("Access denied for path: " + path + ", role: " + role);
                response.sendRedirect("/login");
                return false;
                
            } catch (Exception e) {
                System.out.println("Error processing token: " + e.getMessage());
                response.sendRedirect("/login");
                return false;
            }
        }

        System.out.println("No token found, redirecting to login");
        response.sendRedirect("/login");
        return false;
    }
    
    // Kiểm tra xem đường dẫn có phải là đường dẫn công khai không
    private boolean isPublicPath(String path) {
        return path.equals("/") || 
               path.equals("/home") || 
               path.equals("/login") || 
               path.equals("/register") || 
               path.startsWith("/assets/") || 
               path.startsWith("/vendor/") || 
               path.startsWith("/css/") || 
               path.startsWith("/js/") || 
               path.startsWith("/images/") || 
               path.startsWith("/api/auth/") || 
               path.startsWith("/api/courses/") || 
               path.equals("/error") || 
               path.startsWith("/courses") || 
               path.equals("/category") || 
               path.startsWith("/category/");
    }
} 