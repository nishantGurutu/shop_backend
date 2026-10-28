package com.kisandaily.config;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.web.cors.*;

@Configuration
public class CorsConfig {
  @Bean
  CorsConfigurationSource corsConfigurationSource(
      @Value(
              "${app.cors-origins:${app.cors.allowed-origins:http://localhost:8765,http://127.0.0.1:8765}}")
          String origins) {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::strip).toList());
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    config.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
  }
}
