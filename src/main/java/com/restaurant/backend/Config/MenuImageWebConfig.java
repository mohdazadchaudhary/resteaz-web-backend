package com.restaurant.backend.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class MenuImageWebConfig implements WebMvcConfigurer {
    private final String storageDirectory;

    public MenuImageWebConfig(
            @Value("${app.menu-image.storage-dir:uploads/menu-items}") String storageDirectory) {
        this.storageDirectory = storageDirectory;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(storageDirectory).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/menu-items/**")
                .addResourceLocations(location);
    }
}
