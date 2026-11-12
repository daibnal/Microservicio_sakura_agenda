package com.sakura.agenda.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // Aplica a todos los endpoints
                        .allowedOrigins(
                                "http://localhost:5173",   // acceso a vite
                                "http://localhost:3000",   // (opcional)
                                "https://tu-frontend.com"  // Producción si es que lo subes
                        )
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders("Location") // (opcional) si devuelves Location u otros
                        .allowCredentials(false)      // Si usas cookies o auth con fetch/axios { withCredentials: true } si no false como acá
                        .maxAge(3600);               //gestiona el tiempo en caché
            }
        };
    }
}


