package com.tpouade.multiteatro.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Habilita CORS para el frontend en desarrollo.
 *
 * Sin esto, el navegador bloquea los fetch desde otro origen (Live Server,
 * Vite, etc.), y mucho mas los requests con Authorization, porque primero
 * manda un preflight OPTIONS. Postman no aplica CORS, por eso ahi "funciona".
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                        "http://localhost:5173",
                        "http://localhost:5500",
                        "http://localhost:3000",
                        "http://localhost:4173",
                        "http://127.0.0.1:5173",
                        "http://127.0.0.1:5500",
                        "http://127.0.0.1:3000",
                        "http://127.0.0.1:4173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type")
                .maxAge(3600);
    }
}
