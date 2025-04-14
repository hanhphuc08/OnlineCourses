package com.example.demo.security;

import com.example.demo.service.FirebaseAuthService;
import com.google.firebase.auth.FirebaseAuthException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Component
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    private final FirebaseAuthService firebaseAuthService;

    public FirebaseAuthenticationFilter(@Lazy FirebaseAuthService firebaseAuthService) {
        this.firebaseAuthService = firebaseAuthService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        try {
            String authorizationHeader = request.getHeader("Authorization");

            // Skip authentication for login and register endpoints
            String requestPath = request.getRequestURI();
            if (requestPath.contains("/api/auth/login") || requestPath.contains("/api/auth/register")) {
                filterChain.doFilter(request, response);
                return;
            }

            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                // Extract the token and decode it
                String idToken = authorizationHeader.substring(7).trim();
                try {
                    idToken = URLDecoder.decode(idToken, StandardCharsets.UTF_8.toString());
                } catch (Exception e) {
                    logger.warn("Failed to decode token, using raw token");
                }

                try {
                    // Verify Firebase ID token
                    UserDetails userDetails = firebaseAuthService.verifyIdToken(idToken);
                    
                    if (userDetails != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                } catch (FirebaseAuthException e) {
                    logger.error("Firebase authentication failed", e);
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.getWriter().write("Invalid or expired token");
                    return;
                }
            }
            
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            logger.error("Error in Firebase authentication filter", e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.getWriter().write("Authentication error occurred");
        }
    }
} 