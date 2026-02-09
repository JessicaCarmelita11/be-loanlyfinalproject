package com.example.loanlyFinalProject.config;

import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Configuration for serving uploaded files as static resources */
@Configuration
public class StorageConfig implements WebMvcConfigurer {

  @Value("${file.upload-dir:uploads}")
  private String uploadDir;

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // Serve files from /uploads/** URL pattern
    String uploadPath = Paths.get(uploadDir).toAbsolutePath().toUri().toString();

    registry.addResourceHandler("/uploads/**").addResourceLocations(uploadPath);
  }
}
