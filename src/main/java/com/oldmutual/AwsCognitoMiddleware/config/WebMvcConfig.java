package com.oldmutual.AwsCognitoMiddleware.config;

import com.oldmutual.AwsCognitoMiddleware.interceptor.AppKeyInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration to register interceptors.
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AppKeyInterceptor appKeyInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Register the AppKeyInterceptor for all /api/auth/** endpoints
        registry.addInterceptor(appKeyInterceptor)
                .addPathPatterns("/api/auth/**");
    }
}