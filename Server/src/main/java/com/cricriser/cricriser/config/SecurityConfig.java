package com.cricriser.cricriser.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import static org.springframework.security.config.Customizer.withDefaults;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.cricriser.cricriser.security.JwtAuthFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            // ✅ DISABLE CSRF (JWT BASED)
            .csrf(csrf -> csrf.disable())

            // ✅ ENABLE CORS
            .cors(withDefaults())

            // ✅ STATELESS SESSION
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // ✅ AUTHORIZATION RULES
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/admin/signup",
                    "/api/admin/login",
                    "/api/admin/verify-otp",
                    "/api/admin/forgot-password",
                    "/api/admin/verify-forgot-otp",

                    "/api/player/signup",
                    "/api/player/login",
                    "/api/player/verify-otp",
                    "/api/player/forgot-password",
                    "/api/player/verify-forgot-otp"
                ).permitAll()

                // EVERYTHING ELSE NEEDS LOGIN
                .anyRequest().authenticated()
            )

            // ✅ JWT FILTER
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ✅ PASSWORD ENCODER
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ✅ AUTH MANAGER
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {

        return configuration.getAuthenticationManager();
    }
}
