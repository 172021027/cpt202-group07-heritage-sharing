package com.example.heritage_sharing_api.config;

import java.io.File;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Configure static resources (CSS, JS, images, etc.)
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");

        // Configure the uploads directory to map to the file system.
        String uploadPath = "file:" + System.getProperty("user.dir") + File.separator + "uploads" + File.separator;
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath);
        
        // Configure HTML file 
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/html/")
                .resourceChain(false)
                .addResolver(new ApiPathResourceResolver());
    }

    // Custom resource resolver to exclude API paths
    private static class ApiPathResourceResolver implements org.springframework.web.servlet.resource.ResourceResolver {
        @Override
        public org.springframework.core.io.Resource resolveResource(
                jakarta.servlet.http.HttpServletRequest request, String requestPath, 
                java.util.List<? extends org.springframework.core.io.Resource> locations, 
                org.springframework.web.servlet.resource.ResourceResolverChain chain) {
            // Exclude API paths
            if (requestPath.startsWith("api/")) {
                return null; // Let the request continue to the controller
            }
            return chain.resolveResource(request, requestPath, locations);
        }

        @Override
        public String resolveUrlPath(
                String resourcePath, 
                java.util.List<? extends org.springframework.core.io.Resource> locations, 
                org.springframework.web.servlet.resource.ResourceResolverChain chain) {
            return chain.resolveUrlPath(resourcePath, locations);
        }
    }
}
