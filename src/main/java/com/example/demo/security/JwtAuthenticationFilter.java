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
            System.out.println("Request method: " + request.getMethod());

            String jwt = null;
            String username = null;

            // Kiểm tra token trong header
            String authorizationHeader = request.getHeader("Authorization");
            System.out.println("Authorization header: " + (authorizationHeader != null ? "present" : "not present"));
            
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
                    System.out.println("Username from token: " + username);
                    
                    String role = jwtUtil.extractRole(jwt);
                    System.out.println("Role from token: " + role);
                    
                    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        UserDetails userDetails = this.userService.loadUserByUsername(username);
                        System.out.println("User authorities: " + userDetails.getAuthorities());

                        if (jwtUtil.validateToken(jwt, userDetails)) {
                            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            System.out.println("Authentication set in SecurityContext");
                            
                            // Nếu đang truy cập cart, kiểm tra role
                            if (requestURI.startsWith("/cart")) {
                                boolean hasValidRole = userDetails.getAuthorities().stream()
                                    .anyMatch(a -> a.getAuthority().startsWith("ROLE_"));
                                System.out.println("Cart access - Has valid role: " + hasValidRole);
                            }
                        } else {
                            System.out.println("Token validation failed");
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Error processing token: " + e.getMessage());
                }
            } else {
                System.out.println("No JWT token found in request");
            }

            System.out.println("=== JWT FILTER END ===\n");
            filterChain.doFilter(request, response);
            
        } catch (Exception e) {
            System.out.println("Error in JWT filter: " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/") || 
               path.startsWith("/login") || 
               path.startsWith("/register") || 
               path.startsWith("/assets/") || 
               path.startsWith("/vendor/") || 
               path.startsWith("/css/") || 
               path.startsWith("/js/") || 
               path.startsWith("/images/") || 
               path.startsWith("/api/courses/") || 
               path.equals("/error") || 
               path.startsWith("/courses") || 
               path.equals("/category") || 
               path.startsWith("/category/");
    }
} 