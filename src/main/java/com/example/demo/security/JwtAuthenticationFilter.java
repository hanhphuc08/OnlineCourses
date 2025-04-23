package com.example.demo.security;

import com.example.demo.util.JwtUtil;
import com.example.demo.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Date;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtUtil jwtUtil;
    private final UserService userService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, @Lazy UserService userService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String requestURI = request.getRequestURI();
            System.out.println("\n=== JWT FILTER START ===");
            System.out.println("Processing request: " + requestURI);

            // Kiểm tra xem request có cần xác thực không
            if (shouldNotFilter(request)) {
                System.out.println("Skipping JWT filter for public path: " + requestURI);
                filterChain.doFilter(request, response);
                return;
            }

            String jwt = null;
            String username = null;

            // Kiểm tra token trong header
            String authorizationHeader = request.getHeader("Authorization");
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                jwt = authorizationHeader.substring(7);
                System.out.println("JWT token found in Authorization header");
            } 
            // Kiểm tra token trong query parameter
            else if (request.getParameter("token") != null) {
                jwt = request.getParameter("token");
                System.out.println("JWT token found in query parameter");
            }

            if (jwt != null) {
                try {
                    username = jwtUtil.extractUsername(jwt);
                    String role = jwtUtil.extractRole(jwt);
                    
                    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        UserDetails userDetails = this.userService.loadUserByUsername(username);
                        
                        if (jwtUtil.validateToken(jwt, userDetails)) {
                            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            System.out.println("Authentication set in SecurityContext");
                        } else {
                            System.out.println("Token validation failed");
                            response.sendRedirect("/login");
                            return;
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Error processing token: " + e.getMessage());
                    response.sendRedirect("/login");
                    return;
                }
            } else {
                System.out.println("No JWT token found in request");
                response.sendRedirect("/login");
                return;
            }

            System.out.println("=== JWT FILTER END ===\n");
            filterChain.doFilter(request, response);
            
        } catch (Exception e) {
            System.out.println("Error in JWT filter: " + e.getMessage());
            e.printStackTrace();
            response.sendRedirect("/login");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/") || 
               path.startsWith("/assets/") || 
               path.startsWith("/vendor/") || 
               path.startsWith("/css/") || 
               path.startsWith("/js/") || 
               path.startsWith("/images/") ||
               path.equals("/") ||
               path.equals("/home") ||
               path.equals("/login") ||
               path.equals("/register") ||
               path.equals("/introduce") ||
               path.equals("/schedule") ||
               path.equals("/forgot-password") ||
               path.equals("/category") ||
               path.startsWith("/category/") ||
               path.startsWith("/courses/") ||
               path.startsWith("/course/detail/") ||
               path.equals("/error");
    }
} 