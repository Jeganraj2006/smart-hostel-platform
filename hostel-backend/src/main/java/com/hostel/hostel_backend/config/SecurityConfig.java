package com.hostel.hostel_backend.config;

import com.hostel.hostel_backend.security.JwtAuthFilter;
import com.hostel.hostel_backend.security.RateLimitFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Autowired
    private RateLimitFilter rateLimitFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ✅ Public endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/setup/**").permitAll()
                        
                        // ✅ Role-restricted endpoints
                        .requestMatchers("/api/registrations/**").hasAnyRole("WARDEN", "ADMIN")
                        .requestMatchers("/api/leaves/pending").hasRole("WARDEN")
                        .requestMatchers("/api/leaves/*/approve").hasRole("WARDEN")
                        .requestMatchers("/api/leaves/*/reject").hasRole("WARDEN")
                        .requestMatchers("/api/analytics/**").hasAnyRole("ADMIN", "SUPER_ADMIN", "HOD")
                        .requestMatchers("/api/gate/**").hasAnyRole("SECURITY_GUARD", "WARDEN")
                        .requestMatchers("/api/audit/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/parent/**").hasRole("PARENT")
                        .requestMatchers("/api/resources/**").hasAnyRole("WARDEN", "STAFF", "ADMIN", "SUPER_ADMIN", "HOD")
                        
                        // ✅ Default fallback rule
                        .anyRequest().authenticated()
                )
                .addFilterBefore(rateLimitFilter, LogoutFilter.class)
                .addFilterBefore(jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}