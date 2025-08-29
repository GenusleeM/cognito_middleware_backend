package com.oldmutual.AwsCognitoMiddleware.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response DTO for app configuration details.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppConfigResponse {
    
    private Long id;
    private UUID appKey;
    private String appName;
    private String awsRegion;
    private String userPoolId;
    private String clientId;
    private boolean enabled;
    
    // Client secret is not included in the response for security reasons
}