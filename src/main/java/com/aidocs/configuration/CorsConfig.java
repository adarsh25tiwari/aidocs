package com.aidocs.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class CorsConfig implements WebMvcConfigurer {
    private final AppConfig appConfig;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] allowedOrigins = appConfig.getCors().getAllowedOrigins().split(",");
        String[] allowedMethods = appConfig.getCors().getAllowedMethods().split(",");
        String[] allowedHeaders = appConfig.getCors().getAllowedHeaders().split(",");

        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods(allowedMethods)
                .allowedHeaders(allowedHeaders)
                .allowCredentials(true);
    }
}
