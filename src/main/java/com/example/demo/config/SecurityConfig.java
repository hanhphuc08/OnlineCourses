package com.example.demo.config;

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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UserService userService;

    public SecurityConfig(@Lazy UserService userService) {
        this.userService = userService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        System.out.println("=== CONFIGURING SECURITY ===");
        
        http
            .csrf(csrf -> csrf.disable())
            .cors().configurationSource(corsConfigurationSource())
            .and()
            .authorizeHttpRequests(authorize -> {
                System.out.println("Configuring authorization rules...");
                authorize
                    .requestMatchers(
                        "/verify-email",
                        "/verify-email/**",
                        "/api/auth/verify-email",
                        "/api/auth/resend-verification",
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/auth/logout",
                        "/",
                        "/home",
                        "/assets/**",
                        "/vendor/**",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/login",
                        "/register",
                        "/introduce",
                        "/schedule",
                        "/forgot-password",
                        "/reset-password",
                        "/category/**",
                        "/courses/**",
                        "/course/detail/**",
                        "/error",
                            "/favicon.ico",
                        "/search/**"
                    ).permitAll()
                    .requestMatchers("/profile/**", "/course/cart/add/**", "/course/checkout/**", "/cart/**", "/checkout/vnpay-return").authenticated()
                    .requestMatchers("/staff/**").hasRole("Staff")
                    .requestMatchers("/owner/**").hasRole("Owner")
                    .requestMatchers("/customer/**").hasRole("Customer")
                    .anyRequest().authenticated();
                System.out.println("Authorization rules configured.");
            })
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/api/auth/login")
                        .usernameParameter("emailOrPhone")
                        .passwordParameter("password")
                        .successHandler((request, response, authentication) -> {
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write("{\"status\":\"success\",\"message\":\"Đăng nhập thành công! Chào mừng bạn quay trở lại.\"}");
                        })
                        .failureHandler((request, response, exception) -> {
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write("{\"status\":\"error\",\"message\":\"" + exception.getMessage() + "\"}");
                        })
                        .permitAll()
                )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .clearAuthentication(true)
                .permitAll()
            )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) // Đảm bảo tạo session nếu cần
                        .invalidSessionUrl("/login") // Chuyển hướng nếu session không hợp lệ
                        .maximumSessions(1) // Giới hạn số session
                        .expiredUrl("/login?expired=true")
                )
            .exceptionHandling(exceptionHandling -> exceptionHandling
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    System.out.println("=== ACCESS DENIED ===");
                    System.out.println("Request URI: " + request.getRequestURI());
                    System.out.println("Exception: " + accessDeniedException.getMessage());
                    response.sendRedirect("/login");
                })
                .authenticationEntryPoint((request, response, authException) -> {
                    System.out.println("=== AUTHENTICATION FAILED ===");
                    System.out.println("Request URI: " + request.getRequestURI());
                    System.out.println("Exception: " + authException.getMessage());
                    response.sendRedirect("/login");
                })
            );

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
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "x-requested-with"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
} 