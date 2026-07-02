package org.example.Healthcareplatform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the MVP.
 * <p>
 * Open endpoints so Member 1 (AI) can develop independently.
 * Member 2 will tighten rules once Authentication is implemented.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF — REST APIs use Bearer tokens, not cookies
                .csrf(csrf -> csrf.disable())

                // Stateless sessions — no server-side session for APIs
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // ---- Public AI endpoints (MVP development phase) ----
                        .requestMatchers(HttpMethod.POST, "/api/chat", "/api/chat/**").permitAll()

                        // ---- Allow all while Auth is under construction (Member 2) ----
                        .anyRequest().permitAll()
                )

                // Disable form login / HTTP Basic for now
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }
}
