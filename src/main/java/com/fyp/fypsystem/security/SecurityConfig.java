package com.fyp.fypsystem.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Auth endpoints — always public
                .requestMatchers("/api/auth/**").permitAll()
                // Static frontend resources
                .requestMatchers("/", "/*.html", "/css/**", "/img/**", "/assets/**", "/js/**", "/lib/**",
                                 "/puzzles.json", "/favicon.ico", "/logo.png").permitAll()
                // Public puzzle reading (browsing the catalog without login)
                .requestMatchers(HttpMethod.GET, "/api/puzzles", "/api/puzzles/**",
                                 "/api/puzzles/themes").permitAll()
                // Opening book lookup — public so analysis page works without login
                .requestMatchers(HttpMethod.GET, "/api/openings/lookup").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/coaches").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/analysis").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/payments/checkout/success",
                                 "/api/payments/checkout/cancel").permitAll()
                // Everything else needs a valid JWT
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
