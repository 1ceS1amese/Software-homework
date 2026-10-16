package com.csms.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${CSMS_CORS_ALLOWED_ORIGINS:http://localhost:5173,http://127.0.0.1:5173}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void configureMessageConverters(java.util.List<org.springframework.http.converter.HttpMessageConverter<?>> converters) {
        // Skeleton for SensitiveConverter
        // converters.add(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(
        //     new com.fasterxml.jackson.databind.ObjectMapper()
        //         .registerModule(new com.fasterxml.jackson.databind.module.SimpleModule()
        //             // .addSerializer(String.class, new SensitiveConverter())
        //         )
        // ));
    }
}
