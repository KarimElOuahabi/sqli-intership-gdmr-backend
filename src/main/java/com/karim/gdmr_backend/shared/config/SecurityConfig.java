package com.karim.gdmr_backend.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Unauthorized"))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/admin/users").hasAnyRole("ADMIN", "HR")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        
                        .requestMatchers(HttpMethod.POST, "/api/visits/spontaneous").hasRole("EMPLOYEE")
                        .requestMatchers(HttpMethod.POST, "/api/visits/scheduled").hasRole("HR")
                        .requestMatchers(HttpMethod.POST, "/api/visits/send-reminders").hasAnyRole("ADMIN", "HR")
                        .requestMatchers(HttpMethod.PATCH, "/api/visits/*/assign-slot").hasRole("HR")
                        .requestMatchers(HttpMethod.PATCH, "/api/visits/*/confirm").hasRole("EMPLOYEE")
                        .requestMatchers(HttpMethod.PATCH, "/api/visits/*/employee-reject").hasRole("EMPLOYEE")
                        .requestMatchers(HttpMethod.PATCH, "/api/visits/*/doctor-confirm").hasRole("DOCTOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/visits/*/doctor-reject").hasRole("DOCTOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/visits/*/status").hasRole("DOCTOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/visits/*/report").hasRole("DOCTOR")
                        .requestMatchers(HttpMethod.GET, "/api/visits/*/negotiation-history").hasAnyRole("ADMIN", "HR", "DOCTOR", "EMPLOYEE")
                        .requestMatchers(HttpMethod.GET, "/api/visits/*").hasAnyRole("ADMIN", "HR", "DOCTOR", "EMPLOYEE")
                        .requestMatchers(HttpMethod.GET, "/api/visits").hasAnyRole("ADMIN", "HR", "DOCTOR", "EMPLOYEE")

                        .requestMatchers(HttpMethod.GET, "/api/staff/profile").hasAnyRole("ADMIN", "HR")
                        .requestMatchers(HttpMethod.GET, "/api/staff/profile/*").hasAnyRole("ADMIN", "HR")
                        .requestMatchers(HttpMethod.PUT, "/api/staff/profile").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/staff/profile/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/profile-issues").hasAnyRole("EMPLOYEE", "DOCTOR", "HR")
                        .requestMatchers(HttpMethod.GET, "/api/profile-issues").hasAnyRole("ADMIN", "HR")
                        .requestMatchers(HttpMethod.PATCH, "/api/profile-issues/*/resolve").hasAnyRole("ADMIN", "HR")
                        .requestMatchers("/api/employee/profile").hasRole("EMPLOYEE")
                        .requestMatchers(HttpMethod.GET, "/api/employee/profile/**").hasAnyRole("ADMIN", "HR", "DOCTOR")
                        .requestMatchers("/api/employee/profile/**").hasAnyRole("ADMIN", "HR")
                        .requestMatchers(HttpMethod.GET, "/api/employees/**").hasAnyRole("ADMIN", "HR", "DOCTOR")
                        .requestMatchers("/api/employees/**").hasAnyRole("ADMIN", "HR")
                        .requestMatchers(HttpMethod.PUT, "/api/employee/*").hasAnyRole("ADMIN", "HR")
                        .requestMatchers("/api/doctor/profile").hasRole("DOCTOR")
                        .requestMatchers("/api/doctor/profile/**").hasAnyRole("ADMIN", "HR")
                        .requestMatchers("/api/doctors/**").hasAnyRole("ADMIN", "HR", "EMPLOYEE")
                        .requestMatchers(HttpMethod.PUT, "/api/doctor/*").hasAnyRole("ADMIN", "HR")
                        .requestMatchers("/api/timeslot").hasRole("HR")
                        .requestMatchers("/api/timeslots").hasAnyRole("HR", "EMPLOYEE")
                        .requestMatchers("/api/documents/**").hasAnyRole("DOCTOR", "EMPLOYEE", "HR")
                        .requestMatchers("/api/medical-history/**").hasRole("DOCTOR")
                        .requestMatchers(HttpMethod.GET, "/api/notifications/stream").permitAll()
                        .requestMatchers("/api/notifications/**").hasAnyRole("ADMIN", "HR", "DOCTOR", "EMPLOYEE")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @org.springframework.beans.factory.annotation.Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}