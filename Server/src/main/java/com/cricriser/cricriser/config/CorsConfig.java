package com.cricriser.cricriser.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        // ✅ Frontend origins
        config.setAllowedOrigins(
            List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173"
                // add production URL later
            )
        );

        // ✅ Allow JWT header
        config.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "Accept"
        ));

        // ✅ Required HTTP methods
        config.setAllowedMethods(List.of(
            "GET",
            "POST",
            "PUT",
            "DELETE",
            "OPTIONS"
        ));

        // ✅ Allow cookies / auth headers
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
