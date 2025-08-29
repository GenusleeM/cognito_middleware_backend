package com.oldmutual.AwsCognitoMiddleware.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for creating or updating an app configuration.
 */
@Data
public class AppConfigRequest {
    
    @NotBlank(message = "App name is required")
    private String appName;
    
    @NotBlank(message = "AWS region is required")
    private String awsRegion;
    
    @NotBlank(message = "User pool ID is required")
    private String userPoolId;
    
    @NotBlank(message = "Client ID is required")
    private String clientId;
    
    // Client secret is optional as some Cognito operations only require userPoolId and clientId
    private String clientSecret;
}