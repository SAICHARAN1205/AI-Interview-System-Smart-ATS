package com.aihiringplatform.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json");
                            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write("{\"success\": false, \"message\": \"Your session expired. Please login again.\", \"code\": \"UNAUTHORIZED\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setContentType("application/json");
                            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN);
                            response.getWriter().write("{\"success\": false, \"message\": \"You do not have permission to access this resource.\", \"code\": \"ACCESS_DENIED\"}");
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        // Handle CORS preflight requests
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        // Admin-only endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Recruiter-only endpoints
                        .requestMatchers("/api/jobs/create").hasRole("RECRUITER")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/jobs/{id}").hasRole("RECRUITER")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/jobs/{id}").hasRole("RECRUITER")
                        .requestMatchers("/api/score/**").hasRole("RECRUITER")
                        .requestMatchers("/api/interviews/**").hasRole("RECRUITER")
                        .requestMatchers("/api/resumes/status/**", "/api/resumes/download/**").hasRole("RECRUITER")
                        .requestMatchers("/api/resumes/candidates/**").hasRole("RECRUITER")
                        .requestMatchers("/api/applications/job/**").hasRole("RECRUITER")
                        .requestMatchers("/api/applications/recruiter").hasRole("RECRUITER")
                        .requestMatchers("/api/applications/*/status").hasRole("RECRUITER")
                        .requestMatchers("/api/applications/*/recalculate-score").hasRole("RECRUITER")
                        .requestMatchers("/api/analytics/recruiter", "/api/analytics/recruiter/export.csv").hasRole("RECRUITER")
                        .requestMatchers("/api/analytics/candidate").hasRole("CANDIDATE")
                        // Candidate-only endpoints
                        .requestMatchers("/api/resumes/upload").hasRole("CANDIDATE")
                        .requestMatchers("/api/resumes/me", "/api/resumes/me/download").hasRole("CANDIDATE")
                        .requestMatchers("/api/resumes/analyze").hasRole("CANDIDATE")
                        .requestMatchers("/api/match/**").hasRole("CANDIDATE")
                        .requestMatchers("/api/interview/**").hasRole("CANDIDATE")
                        .requestMatchers("/api/applications/apply/**").hasRole("CANDIDATE")
                        .requestMatchers("/api/applications/candidate").hasRole("CANDIDATE")
                        .requestMatchers("/api/ai/interview/**").hasRole("CANDIDATE")
                        .requestMatchers("/api/ai/ats/**").hasRole("CANDIDATE")
                        .requestMatchers("/api/ai/match/**").authenticated()
                        // Public endpoints
                        .requestMatchers("/api/users/register", "/api/auth/login", "/api/auth/captcha", "/api/auth/verify-registration-otp", "/api/auth/resend-registration-otp", "/api/auth/forgot-password", "/api/auth/verify-reset-otp", "/api/auth/reset-password", "/api/jobs/all", "/api/jobs/{id}", "/api/test", "/api/test/**", "/error").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
