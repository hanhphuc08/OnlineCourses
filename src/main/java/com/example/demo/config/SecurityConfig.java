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
            .csrf().disable()
            .cors().configurationSource(corsConfigurationSource())
            .and()
            .authorizeHttpRequests(authorize -> {
                System.out.println("Configuring authorization rules...");
                authorize
                    .requestMatchers(
                        "/api/auth/**",
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
                        "/error"
                    ).permitAll()
                    .requestMatchers("/profile/**", "/course/cart/add/**", "/course/checkout/**", "/cart/**").authenticated()
                    .requestMatchers("/staff/**").hasRole("STAFF")
                    .requestMatchers("/owner/**").hasRole("OWNER")
                    .requestMatchers("/customer/**").hasRole("CUSTOMER")
                    .anyRequest().authenticated();
                System.out.println("Authorization rules configured.");
            })
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/api/auth/login")
                .usernameParameter("emailOrPhone")
                .passwordParameter("password")
                .defaultSuccessUrl("/")
                .failureUrl("/login?error=true")
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
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
} 