package com.jrcr.gestorofertasweb.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Usar la misma ruta absoluta que OfertaService
        String workingDir = System.getProperty("user.dir");
        Path uploadPath = Paths.get(workingDir, "uploads").toAbsolutePath().normalize();
        String uploadDir = uploadPath.toFile().getAbsolutePath();

        System.out.println("=== WebConfig: Sirviendo archivos desde: " + uploadDir + " ===");

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }
}