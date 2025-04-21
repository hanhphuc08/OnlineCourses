package com.example.demo.config;

import com.example.demo.security.JwtAuthenticationFilter;
import com.example.demo.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UserService userService;
    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(@Lazy UserService userService, 
                         @Lazy JwtAuthenticationFilter jwtAuthFilter) {
        this.userService = userService;
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        System.out.println("=== CONFIGURING SECURITY ===");
        
        http
            .csrf().disable()
            .cors().configurationSource(corsConfigurationSource())
            .and()
            .authorizeHttpRequests(authorize -> {
                System.out.println("Configuring authorization rules...");
                authorize
                    .requestMatchers(
                        "/api/auth/**",
                        "/api/courses/**",
                        "/",
                        "/home",
                        "/assets/**",
                        "/vendor/**",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/login",
                        "/register",
                        "/login.html",
                        "/register.html",
                        "/auth/**",
                        "/category/**",
                        "/category",
                        "/courses/**",
                        "/courses",
                        "/error",
                        "/api/auth/logout"
                    ).permitAll()
                    .requestMatchers("/cart", "/cart/**").authenticated()
                    .requestMatchers("/staff/**").hasRole("STAFF")
                    .requestMatchers("/owner/**").hasRole("OWNER")
                    .requestMatchers("/customer/**").hasRole("CUSTOMER")
                    .anyRequest().authenticated();
                System.out.println("Authorization rules configured.");
            })
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .exceptionHandling(exceptionHandling -> exceptionHandling
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    System.out.println("=== ACCESS DENIED ===");
                    System.out.println("Request URI: " + request.getRequestURI());
                    System.out.println("Exception: " + accessDeniedException.getMessage());
                    
                    String requestedWithHeader = request.getHeader("X-Requested-With");
                    if ("XMLHttpRequest".equals(requestedWithHeader)) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.getWriter().write("Access denied");
                    } else {
                        response.sendRedirect("/login");
                    }
                })
                .authenticationEntryPoint((request, response, authException) -> {
                    System.out.println("=== AUTHENTICATION FAILED ===");
                    System.out.println("Request URI: " + request.getRequestURI());
                    System.out.println("Exception: " + authException.getMessage());
                    
                    String requestedWithHeader = request.getHeader("X-Requested-With");
                    if ("XMLHttpRequest".equals(requestedWithHeader)) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write("Unauthorized");
                    } else {
                        response.sendRedirect("/login");
                    }
                })
            );

        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        System.out.println("=== SECURITY CONFIGURATION COMPLETE ===");

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
} 