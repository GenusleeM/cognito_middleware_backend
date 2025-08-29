package com.oldmutual.AwsCognitoMiddleware.service;

import com.oldmutual.AwsCognitoMiddleware.model.CognitoAppConfig;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for AWS Cognito operations.
 * Dynamically creates Cognito clients based on the configuration from the request.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CognitoService {

    private static final String APP_CONFIG_ATTRIBUTE = "appConfig";
    private static final String AUTH_FLOW = "ADMIN_USER_PASSWORD_AUTH";

    /**
     * Get the current request's Cognito configuration.
     *
     * @return The Cognito configuration for the current request
     * @throws IllegalStateException if the configuration is not found
     */
    private CognitoAppConfig getCurrentAppConfig() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new IllegalStateException("Request attributes not available");
        }

        HttpServletRequest request = attributes.getRequest();
        CognitoAppConfig appConfig = (CognitoAppConfig) request.getAttribute(APP_CONFIG_ATTRIBUTE);
        
        if (appConfig == null) {
            throw new IllegalStateException("Cognito app configuration not found in request");
        }
        
        return appConfig;
    }

    /**
     * Create a Cognito client for the current request.
     *
     * @return A configured CognitoIdentityProviderClient
     */
    private CognitoIdentityProviderClient createCognitoClient() {
        CognitoAppConfig appConfig = getCurrentAppConfig();
        
        // Create Cognito client with the region and anonymous credentials
        // This prevents the SDK from looking for AWS credentials
        return CognitoIdentityProviderClient.builder()
                .region(Region.of(appConfig.getAwsRegion()))
                .credentialsProvider(AnonymousCredentialsProvider.create())
                .build();
    }

    /**
     * Register a new user in Cognito.
     *
     * @param email      The user's email
     * @param password   The user's password
     * @param attributes Map of custom attributes for the user
     * @return The result of the registration
     */
    public AdminCreateUserResponse registerUser(String email, String password, Map<String, String> attributes) {
        CognitoAppConfig appConfig = getCurrentAppConfig();
        CognitoIdentityProviderClient cognitoClient = createCognitoClient();
        
        try {
            // Create a list to hold all user attributes
            java.util.List<AttributeType> userAttributes = new java.util.ArrayList<>();
            
            // Add required attributes
            userAttributes.add(AttributeType.builder()
                    .name("email")
                    .value(email)
                    .build());
            
            userAttributes.add(AttributeType.builder()
                    .name("email_verified")
                    .value("true")
                    .build());
            
            // Add custom attributes if provided
            if (attributes != null && !attributes.isEmpty()) {
                for (Map.Entry<String, String> entry : attributes.entrySet()) {
                    if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                        userAttributes.add(AttributeType.builder()
                                .name(entry.getKey())
                                .value(entry.getValue())
                                .build());
                    }
                }
            }
            
            // Create the user
            AdminCreateUserRequest createUserRequest = AdminCreateUserRequest.builder()
                    .userPoolId(appConfig.getUserPoolId())
                    .username(email)
                    .temporaryPassword(password)
                    .userAttributes(userAttributes)
                    .messageAction(MessageActionType.SUPPRESS) // Don't send welcome email
                    .build();
            
            AdminCreateUserResponse createUserResponse = cognitoClient.adminCreateUser(createUserRequest);
            
            // Set the permanent password
            AdminSetUserPasswordRequest setPasswordRequest = AdminSetUserPasswordRequest.builder()
                    .userPoolId(appConfig.getUserPoolId())
                    .username(email)
                    .password(password)
                    .permanent(true)
                    .build();
            
            cognitoClient.adminSetUserPassword(setPasswordRequest);
            
            return createUserResponse;
        } finally {
            cognitoClient.close();
        }
    }

    /**
     * Verify a user's account with a confirmation code.
     *
     * @param email            The user's email
     * @param confirmationCode The confirmation code
     * @return The result of the verification
     */
    public ConfirmSignUpResponse verifyUser(String email, String confirmationCode) {
        CognitoAppConfig appConfig = getCurrentAppConfig();
        CognitoIdentityProviderClient cognitoClient = createCognitoClient();
        
        try {
            ConfirmSignUpRequest confirmSignUpRequest = ConfirmSignUpRequest.builder()
                    .clientId(appConfig.getClientId())
                    .username(email)
                    .confirmationCode(confirmationCode)
                    .build();
            
            return cognitoClient.confirmSignUp(confirmSignUpRequest);
        } finally {
            cognitoClient.close();
        }
    }

    /**
     * Authenticate a user with email and password.
     *
     * @param email    The user's email
     * @param password The user's password
     * @return The authentication result
     */
    public AdminInitiateAuthResponse authenticateUser(String email, String password) {
        CognitoAppConfig appConfig = getCurrentAppConfig();
        CognitoIdentityProviderClient cognitoClient = createCognitoClient();
        
        try {
            Map<String, String> authParams = new HashMap<>();
            authParams.put("USERNAME", email);
            authParams.put("PASSWORD", password);
            
            AdminInitiateAuthRequest authRequest = AdminInitiateAuthRequest.builder()
                    .authFlow(AUTH_FLOW)
                    .clientId(appConfig.getClientId())
                    .userPoolId(appConfig.getUserPoolId())
                    .authParameters(authParams)
                    .build();
            
            return cognitoClient.adminInitiateAuth(authRequest);
        } finally {
            cognitoClient.close();
        }
    }

    /**
     * Initiate the forgot password flow.
     *
     * @param email The user's email
     * @return The result of the forgot password request
     */
    public ForgotPasswordResponse forgotPassword(String email) {
        CognitoAppConfig appConfig = getCurrentAppConfig();
        CognitoIdentityProviderClient cognitoClient = createCognitoClient();
        
        try {
            ForgotPasswordRequest forgotPasswordRequest = ForgotPasswordRequest.builder()
                    .clientId(appConfig.getClientId())
                    .username(email)
                    .build();
            
            return cognitoClient.forgotPassword(forgotPasswordRequest);
        } finally {
            cognitoClient.close();
        }
    }

    /**
     * Confirm a new password with the confirmation code.
     *
     * @param email            The user's email
     * @param confirmationCode The confirmation code
     * @param newPassword      The new password
     * @return The result of the confirmation
     */
    public ConfirmForgotPasswordResponse confirmForgotPassword(String email, String confirmationCode, String newPassword) {
        CognitoAppConfig appConfig = getCurrentAppConfig();
        CognitoIdentityProviderClient cognitoClient = createCognitoClient();
        
        try {
            ConfirmForgotPasswordRequest confirmRequest = ConfirmForgotPasswordRequest.builder()
                    .clientId(appConfig.getClientId())
                    .username(email)
                    .confirmationCode(confirmationCode)
                    .password(newPassword)
                    .build();
            
            return cognitoClient.confirmForgotPassword(confirmRequest);
        } finally {
            cognitoClient.close();
        }
    }
}