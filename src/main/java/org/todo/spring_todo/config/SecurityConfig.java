package org.todo.spring_todo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SESSION 4 — Security is a chain of filters that every request walks through
 * BEFORE it reaches a controller. This class configures that chain.
 */
@Configuration
@EnableMethodSecurity   // turns on @PreAuthorize on methods
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. WHO may access WHAT. Rules are checked top to bottom; first match wins.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers("/h2-console/**", "/graphiql/**", "/error").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())

                // 2. HOW clients prove who they are: "Authorization: Basic base64(user:pass)"
                .httpBasic(Customizer.withDefaults())

                // 3. A REST API remembers nothing between requests -> no HTTP session.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. CSRF protects browser cookie sessions. We don't use cookies, so we turn it off.
                .csrf(csrf -> csrf.disable())

                // 5. The H2 console UI uses <frame>s; allow frames from our own origin.
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt is slow ON PURPOSE and adds a random salt: identical passwords get different hashes.
        return new BCryptPasswordEncoder();
    }
}
