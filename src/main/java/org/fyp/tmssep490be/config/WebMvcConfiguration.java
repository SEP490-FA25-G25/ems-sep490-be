package org.fyp.tmssep490be.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web MVC Configuration for Spring Data Web Support
 *
 * Enables stable JSON serialization for Page objects using DTO mode
 * This prevents warnings about unstable PageImpl serialization
 */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        // Configure pageable resolver with default page size and properties
        PageableHandlerMethodArgumentResolver resolver = new PageableHandlerMethodArgumentResolver();
        resolver.setOneIndexedParameters(false); // Use 0-based page indexing
        resolver.setFallbackPageable(PageRequest.of(0, 20)); // Default: page 0, size 20
        resolver.setMaxPageSize(100); // Maximum page size to prevent performance issues
        resolvers.add(resolver);
    }
}