package com.trainning.movie_booking_system.config;

import com.trainning.movie_booking_system.security.CustomUserDetailsService;
import com.trainning.movie_booking_system.security.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtFilter jwtFilter;

    /**
     * Public endpoints - không cần authentication
     * Bao gồm:
     * - Authentication endpoints (register, login, etc.)
     * - Public read-only resources (movies, theaters, showtimes - GET only)
     * - Payment webhooks (verified by signature)
     * - API documentation
     */
    public static final String[] PUBLIC_ENDPOINTS = {
            "/",
            // ===== AUTHENTICATION =====
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/logout",
            "/api/v1/auth/activate",
            "/api/v1/auth/refresh-token",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/otp/**",
            
            // ===== PAYMENT WEBHOOKS (verified by signature) =====
            "/api/v1/payments/vnpay/callback",
            "/api/v1/payments/webhook",
            
            // ===== API DOCUMENTATION =====
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authz -> authz
                        // ===== PUBLIC ENDPOINTS =====
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        
                        // ===== PUBLIC READ-ONLY (GET only) =====
                        // Movies - public can view, search
                        .requestMatchers(HttpMethod.GET, "/api/v1/movies/**").permitAll()
                        
                        // Theaters - public can view
                        .requestMatchers(HttpMethod.GET, "/api/v1/theaters/**").permitAll()
                        
                        // Showtimes - public can view schedule
                        .requestMatchers(HttpMethod.GET, "/api/v1/showtimes/**").permitAll()
                        
                        // Screens & Seats - public can view layout
                        .requestMatchers(HttpMethod.GET, "/api/v1/screens/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/seats/**").permitAll()
                        
                        // Vouchers - public can view available vouchers
                        .requestMatchers(HttpMethod.GET, "/api/v1/vouchers").permitAll()
                        
                        // ===== ADMIN ENDPOINTS =====
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        
                        // ===== AUTHENTICATED ENDPOINTS =====
                        // Bookings - require authentication
                        .requestMatchers("/api/v1/bookings/**").authenticated()
                        
                        // Payments - require authentication (except webhooks already in PUBLIC_ENDPOINTS)
                        .requestMatchers("/api/v1/payments/**").authenticated()
                        
                        // Seat holds - require authentication
                        .requestMatchers("/api/v1/seat-holds/**").authenticated()
                        
                        // Voucher operations - require authentication
                        .requestMatchers("/api/v1/vouchers/validate").authenticated()
                        .requestMatchers("/api/v1/voucher-usages/**").authenticated()

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}