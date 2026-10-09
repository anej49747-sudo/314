package com.chef.chef.config;

@org.springframework.context.annotation.Configuration
public class CorsConfig {

    @org.springframework.context.annotation.Bean
    public org.springframework.web.filter.CorsFilter corsFilter() {

        org.springframework.web.cors.CorsConfiguration config =
                new org.springframework.web.cors.CorsConfiguration();

        config.setAllowCredentials(true);
        config.addAllowedOrigin("http://localhost:5174");
        config.addAllowedHeader("*");

        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("PATCH");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("OPTIONS");

        org.springframework.web.cors.UrlBasedCorsConfigurationSource source =
                new org.springframework.web.cors.UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return new org.springframework.web.filter.CorsFilter(source);
    }
}