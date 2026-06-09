package com.AQARK.AQARK_RENT_MANAGEMENT.Common_Utilities;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class MediaResourceConfig implements WebMvcConfigurer {

    @Value("${upload.directory}")
    private String uploadDirectory;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        Path uploadPath = Paths.get(uploadDirectory)
                .toAbsolutePath()
                .normalize();

        registry.addResourceHandler("/api/media/**")
                .addResourceLocations(uploadPath.toUri().toString());
    }
}
