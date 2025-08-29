package com.oldmutual.AwsCognitoMiddleware.interceptor;

import com.oldmutual.AwsCognitoMiddleware.model.CognitoAppConfig;
import com.oldmutual.AwsCognitoMiddleware.repository.CognitoAppConfigRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;
import java.util.UUID;

/**
 * Interceptor to validate the X-APP-KEY header and fetch the corresponding Cognito configuration.
 * This interceptor is applied to all requests to /api/auth/** endpoints.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppKeyInterceptor implements HandlerInterceptor {

    private static final String APP_KEY_HEADER = "X-APP-KEY";
    private static final String APP_CONFIG_ATTRIBUTE = "appConfig";

    private final CognitoAppConfigRepository cognitoAppConfigRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String appKeyHeader = request.getHeader(APP_KEY_HEADER);
        
        // Check if the X-APP-KEY header is present
        if (appKeyHeader == null || appKeyHeader.isEmpty()) {
            log.error("Missing X-APP-KEY header");
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Missing X-APP-KEY header");
            return false;
        }
        
        try {
            // Parse the UUID from the header
            UUID appKey = UUID.fromString(appKeyHeader);
            
            // Fetch the configuration from the database
            Optional<CognitoAppConfig> appConfigOpt = cognitoAppConfigRepository.findByAppKey(appKey);
            
            if (appConfigOpt.isEmpty()) {
                log.error("Invalid X-APP-KEY: {}", appKeyHeader);
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.getWriter().write("Invalid X-APP-KEY");
                return false;
            }
            
            CognitoAppConfig appConfig = appConfigOpt.get();
            
            // Check if the app is enabled
            if (!appConfig.isEnabled()) {
                log.error("App is disabled: {}", appKeyHeader);
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.getWriter().write("App is disabled");
                return false;
            }
            
            // Store the configuration in the request attributes for later use
            request.setAttribute(APP_CONFIG_ATTRIBUTE, appConfig);
            log.debug("Found configuration for app: {}", appConfig.getAppName());
            
            return true;
        } catch (IllegalArgumentException e) {
            // Invalid UUID format
            log.error("Invalid X-APP-KEY format: {}", appKeyHeader);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Invalid X-APP-KEY format");
            return false;
        }
    }
}