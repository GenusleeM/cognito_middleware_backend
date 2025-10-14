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
 * 
 * @author Genuslee Mapedze
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CognitoService {

    private static final String APP_CONFIG_ATTRIBUTE = "appConfig";
    private static final String AUTH_FLOW = "USER_PASSWORD_AUTH";
    
    private final UserActivityLogService userActivityLogService;

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
    public SignUpResponse registerUser(String email, String password, Map<String, String> attributes) {
        log.info("Registering new user with email: {}", email);
        CognitoAppConfig appConfig = getCurrentAppConfig();
        log.debug("Using app config: {}", appConfig.getAppName());
        CognitoIdentityProviderClient cognitoClient = createCognitoClient();
        
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        
        try {
            // Create a list to hold all user attributes
            java.util.List<AttributeType> userAttributes = new java.util.ArrayList<>();
            
            // Add required attributes
            userAttributes.add(AttributeType.builder()
                    .name("email")
                    .value(email)
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
            
            // Create the user using SignUp instead of AdminCreateUser
            SignUpRequest signUpRequest = SignUpRequest.builder()
                    .clientId(appConfig.getClientId())
                    .username(email)
                    .password(password)
                    .userAttributes(userAttributes)
                    .build();
            
            try {
                var response = cognitoClient.signUp(signUpRequest);
                log.info("User registration successful: {}", response);
                
                // Log successful registration
                userActivityLogService.logActivity(
                    "REGISTER",
                    email,
                    appConfig.getUserPoolId(),
                    appConfig.getAppName(),
                    "SUCCESS",
                    null,
                    request
                );
                
                return response;
            } catch (CognitoIdentityProviderException ex) {
                String errorMessage = ex.getMessage();
                if (errorMessage.contains("(Service:")) {
                    errorMessage = errorMessage.substring(0, errorMessage.indexOf("(Service:"));
                }
                
                // Log failed registration
                userActivityLogService.logActivity(
                    "REGISTER",
                    email,
                    appConfig.getUserPoolId(),
                    appConfig.getAppName(),
                    "FAILURE",
                    errorMessage,
                    request
                );
                
                throw ex;
            }
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
        log.info("Verifying user with email: {}", email);
        CognitoAppConfig appConfig = getCurrentAppConfig();
        log.debug("Using app config: {}", appConfig.getAppName());
        CognitoIdentityProviderClient cognitoClient = createCognitoClient();
        
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        
        try {
            ConfirmSignUpRequest confirmSignUpRequest = ConfirmSignUpRequest.builder()
                    .clientId(appConfig.getClientId())
                    .username(email)
                    .confirmationCode(confirmationCode)
                    .build();
            
            try {
                var response = cognitoClient.confirmSignUp(confirmSignUpRequest);
                log.info("User verification successful for: {}", email);
                
                // Log successful verification
                userActivityLogService.logActivity(
                    "VERIFY",
                    email,
                    appConfig.getUserPoolId(),
                    appConfig.getAppName(),
                    "SUCCESS",
                    null,
                    request
                );
                
                return response;
            } catch (CognitoIdentityProviderException ex) {
                String errorMessage = ex.getMessage();
                if (errorMessage.contains("(Service:")) {
                    errorMessage = errorMessage.substring(0, errorMessage.indexOf("(Service:"));
                }
                
                // Log failed verification
                userActivityLogService.logActivity(
                    "VERIFY",
                    email,
                    appConfig.getUserPoolId(),
                    appConfig.getAppName(),
                    "FAILURE",
                    errorMessage,
                    request
                );
                
                throw ex;
            }
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
    public InitiateAuthResponse authenticateUser(String email, String password) {
        log.info("Authenticating user with email: {}", email);
        CognitoAppConfig appConfig = getCurrentAppConfig();
        log.debug("Using app config: {}", appConfig.getAppName());
        CognitoIdentityProviderClient cognitoClient = createCognitoClient();
        
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        
        try {
            Map<String, String> authParams = new HashMap<>();
            authParams.put("USERNAME", email);
            authParams.put("PASSWORD", password);
            
            InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                    .authFlow(AUTH_FLOW)
                    .clientId(appConfig.getClientId())
                    .authParameters(authParams)
                    .build();
            
            try {
                var response = cognitoClient.initiateAuth(authRequest);
                log.info("User authentication successful for: {}", email);
                
                // Log successful login
                userActivityLogService.logActivity(
                    "LOGIN",
                    email,
                    appConfig.getUserPoolId(),
                    appConfig.getAppName(),
                    "SUCCESS",
                    null,
                    request
                );
                
                return response;
            } catch (CognitoIdentityProviderException ex) {
                String errorMessage = ex.getMessage();
                if (errorMessage.contains("(Service:")) {
                    errorMessage = errorMessage.substring(0, errorMessage.indexOf("(Service:"));
                }
                
                // Log failed login
                userActivityLogService.logActivity(
                    "LOGIN",
                    email,
                    appConfig.getUserPoolId(),
                    appConfig.getAppName(),
                    "FAILURE",
                    errorMessage,
                    request
                );
                
                throw ex;
            }
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
        log.info("Initiating forgot password process for user with email: {}", email);
        CognitoAppConfig appConfig = getCurrentAppConfig();
        log.debug("Using app config: {}", appConfig.getAppName());
        CognitoIdentityProviderClient cognitoClient = createCognitoClient();
        
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        
        try {
            ForgotPasswordRequest forgotPasswordRequest = ForgotPasswordRequest.builder()
                    .clientId(appConfig.getClientId())
                    .username(email)
                    .build();
            
            try {
                var response = cognitoClient.forgotPassword(forgotPasswordRequest);
                log.info("Forgot password request successful for: {}", email);
                
                // Log successful forgot password request
                userActivityLogService.logActivity(
                    "FORGOT_PASSWORD_REQUEST",
                    email,
                    appConfig.getUserPoolId(),
                    appConfig.getAppName(),
                    "SUCCESS",
                    null,
                    request
                );
                
                return response;
            } catch (CognitoIdentityProviderException ex) {
                String errorMessage = ex.getMessage();
                if (errorMessage.contains("(Service:")) {
                    errorMessage = errorMessage.substring(0, errorMessage.indexOf("(Service:"));
                }
                
                // Log failed forgot password request
                userActivityLogService.logActivity(
                    "FORGOT_PASSWORD_REQUEST",
                    email,
                    appConfig.getUserPoolId(),
                    appConfig.getAppName(),
                    "FAILURE",
                    errorMessage,
                    request
                );
                
                throw ex;
            }
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
        log.info("Confirming forgot password for user with email: {}", email);
        CognitoAppConfig appConfig = getCurrentAppConfig();
        log.debug("Using app config: {}", appConfig.getAppName());
        CognitoIdentityProviderClient cognitoClient = createCognitoClient();

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

        try {
            ConfirmForgotPasswordRequest confirmRequest = ConfirmForgotPasswordRequest.builder()
                    .clientId(appConfig.getClientId())
                    .username(email)
                    .confirmationCode(confirmationCode)
                    .password(newPassword)
                    .build();

            try {
                var response = cognitoClient.confirmForgotPassword(confirmRequest);
                log.info("Password reset confirmation successful for: {}", email);

                // Log successful password reset
                userActivityLogService.logActivity(
                    "RESET_PASSWORD",
                    email,
                    appConfig.getUserPoolId(),
                    appConfig.getAppName(),
                    "SUCCESS",
                    null,
                    request
                );

                return response;
            } catch (CognitoIdentityProviderException ex) {
                String errorMessage = ex.getMessage();
                if (errorMessage.contains("(Service:")) {
                    errorMessage = errorMessage.substring(0, errorMessage.indexOf("(Service:"));
                }

                // Log failed password reset
                userActivityLogService.logActivity(
                    "RESET_PASSWORD",
                    email,
                    appConfig.getUserPoolId(),
                    appConfig.getAppName(),
                    "FAILURE",
                    errorMessage,
                    request
                );

                throw ex;
            }
        } finally {
            cognitoClient.close();
        }
    }

    /**
     * Set user MFA preference (SMS or SOFTWARE_TOKEN_MFA).
     *
     * @param accessToken The user's access token
     * @param mfaType     The MFA type (SMS_MFA or SOFTWARE_TOKEN_MFA)
     * @param phoneNumber The phone number for SMS MFA (optional)
     */
    public void setUserMfaPreference(String accessToken, String mfaType, String phoneNumber) {
        log.info("Setting user MFA preference to: {}", mfaType);
        CognitoAppConfig appConfig = getCurrentAppConfig();
        log.debug("Using app config: {}", appConfig.getAppName());
        CognitoIdentityProviderClient cognitoClient = createCognitoClient();

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

        try {
            SetUserMfaPreferenceRequest.Builder requestBuilder = SetUserMfaPreferenceRequest.builder()
                    .accessToken(accessToken);

            if ("SMS_MFA".equals(mfaType)) {
                // Set SMS MFA as preferred
                requestBuilder.smsMfaSettings(SMSMfaSettingsType.builder()
                        .enabled(true)
                        .preferredMfa(true)
                        .build());

                // Update phone number if provided
                if (phoneNumber != null && !phoneNumber.isEmpty()) {
                    updateUserPhoneNumber(accessToken, phoneNumber);
                }
            } else if ("SOFTWARE_TOKEN_MFA".equals(mfaType)) {
                // Set Software Token MFA as preferred
                requestBuilder.softwareTokenMfaSettings(SoftwareTokenMfaSettingsType.builder()
                        .enabled(true)
                        .preferredMfa(true)
                        .build());
            }

            try {
                cognitoClient.setUserMFAPreference(requestBuilder.build());
                log.info("MFA preference set successfully");

                // Log successful MFA setup
                userActivityLogService.logActivity(
                    "MFA_SETUP",
                    "user",
                    appConfig.getUserPoolId(),
                    appConfig.getAppName(),
                    "SUCCESS",
                    "MFA type: " + mfaType,
                    request
                );
            } catch (CognitoIdentityProviderException ex) {
                String errorMessage = ex.getMessage();
                if (errorMessage.contains("(Service:")) {
                    errorMessage = errorMessage.substring(0, errorMessage.indexOf("(Service:"));
                }

                // Log failed MFA setup
                userActivityLogService.logActivity(
                    "MFA_SETUP",
                    "user",
                    appConfig.getUserPoolId(),
                    appConfig.getAppName(),
                    "FAILURE",
                    errorMessage,
                    request
                );

                throw ex;
            }
        } finally {
            cognitoClient.close();
        }
    }

    /**
     * Update user's phone number attribute.
     *
     * @param accessToken The user's access token
     * @param phoneNumber The phone number to set
     */
    private void updateUserPhoneNumber(String accessToken, String phoneNumber) {
        log.info("Updating user phone number");
        CognitoIdentityProviderClient cognitoClient = createCognitoClient();

        try {
            UpdateUserAttributesRequest updateRequest = UpdateUserAttributesRequest.builder()
                    .accessToken(accessToken)
                    .userAttributes(AttributeType.builder()
                            .name("phone_number")
                            .value(phoneNumber)
                            .build())
                    .build();

            cognitoClient.updateUserAttributes(updateRequest);
            log.info("Phone number updated successfully");
        } finally {
            cognitoClient.close();
        }
    }

    /**
     * Associate software token for TOTP MFA.
     *
     * @param accessToken The user's access token
     * @return The associate software token response containing the secret code
     */
    public AssociateSoftwareTokenResponse associateSoftwareToken(String accessToken) {
        log.info("Associating software token for MFA");
        CognitoAppConfig appConfig = getCurrentAppConfig();
        log.debug("Using app config: {}", appConfig.getAppName());
        CognitoIdentityProviderClient cognitoClient = createCognitoClient();

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

        try {
            AssociateSoftwareTokenRequest tokenRequest = AssociateSoftwareTokenRequest.builder()
                    .accessToken(accessToken)
                    .build();

            try {
                var response = cognitoClient.associateSoftwareToken(tokenRequest);
                log.info("Software token associated successfully");

                // Log successful token association
                userActivityLogService.logActivity(
                    "MFA_TOKEN_ASSOCIATE",
                    "user",
                    appConfig.getUserPoolId(),
                    appConfig.getAppName(),
                    "SUCCESS",
                    null,
                    request
                );

                return response;
            } catch (CognitoIdentityProviderException ex) {
                String errorMessage = ex.getMessage();
                if (errorMessage.contains("(Service:")) {
                    errorMessage = errorMessage.substring(0, errorMessage.indexOf("(Service:"));
                }

                // Log failed token association
                userActivityLogService.logActivity(
                    "MFA_TOKEN_ASSOCIATE",
                    "user",
                    appConfig.getUserPoolId(),
                    appConfig.getAppName(),
                    "FAILURE",
                    errorMessage,
                    request
                );

                throw ex;
            }
        } finally {
            cognitoClient.close();
        }
    }

    /**
     * Verify software token for TOTP MFA.
     *
     * @param accessToken The user's access token
     * @param userCode    The TOTP code from the authenticator app
     * @param deviceName  The friendly name for the device
     * @return The verify software token response
     */
    public VerifySoftwareTokenResponse verifySoftwareToken(String accessToken, String userCode, String deviceName) {
        log.info("Verifying software token for MFA");
        CognitoAppConfig appConfig = getCurrentAppConfig();
        log.debug("Using app config: {}", appConfig.getAppName());
        CognitoIdentityProviderClient cognitoClient = createCognitoClient();

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

        try {
            VerifySoftwareTokenRequest verifyRequest = VerifySoftwareTokenRequest.builder()
                    .accessToken(accessToken)
                    .userCode(userCode)
                    .friendlyDeviceName(deviceName)
                    .build();

            try {
                var response = cognitoClient.verifySoftwareToken(verifyRequest);
                log.info("Software token verified successfully");

                // Log successful token verification
                userActivityLogService.logActivity(
                    "MFA_TOKEN_VERIFY",
                    "user",
                    appConfig.getUserPoolId(),
                    appConfig.getAppName(),
                    "SUCCESS",
                    null,
                    request
                );

                return response;
            } catch (CognitoIdentityProviderException ex) {
                String errorMessage = ex.getMessage();
                if (errorMessage.contains("(Service:")) {
                    errorMessage = errorMessage.substring(0, errorMessage.indexOf("(Service:"));
                }

                // Log failed token verification
                userActivityLogService.logActivity(
                    "MFA_TOKEN_VERIFY",
                    "user",
                    appConfig.getUserPoolId(),
                    appConfig.getAppName(),
                    "FAILURE",
                    errorMessage,
                    request
                );

                throw ex;
            }
        } finally {
            cognitoClient.close();
        }
    }

    /**
     * Respond to MFA challenge during authentication.
     *
     * @param email         The user's email
     * @param session       The session from the initial auth response
     * @param mfaCode       The MFA code received via SMS or generated by TOTP
     * @param challengeName The challenge name (SMS_MFA or SOFTWARE_TOKEN_MFA)
     * @return The authentication result with tokens
     */
    public RespondToAuthChallengeResponse respondToMfaChallenge(String email, String session, String mfaCode, String challengeName) {
        log.info("Responding to MFA challenge for user: {}", email);
        CognitoAppConfig appConfig = getCurrentAppConfig();
        log.debug("Using app config: {}", appConfig.getAppName());
        CognitoIdentityProviderClient cognitoClient = createCognitoClient();

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

        try {
            Map<String, String> challengeResponses = new HashMap<>();
            challengeResponses.put("USERNAME", email);

            if ("SMS_MFA".equals(challengeName)) {
                challengeResponses.put("SMS_MFA_CODE", mfaCode);
            } else if ("SOFTWARE_TOKEN_MFA".equals(challengeName)) {
                challengeResponses.put("SOFTWARE_TOKEN_MFA_CODE", mfaCode);
            } else if ("EMAIL_OTP".equals(challengeName)) {
                challengeResponses.put("EMAIL_OTP_CODE", mfaCode);
            } else {
                log.warn("Unknown challenge name: {}. Using default code parameter.", challengeName);
                challengeResponses.put("ANSWER", mfaCode);
            }

            RespondToAuthChallengeRequest challengeRequest = RespondToAuthChallengeRequest.builder()
                    .clientId(appConfig.getClientId())
                    .challengeName(ChallengeNameType.fromValue(challengeName))
                    .session(session)
                    .challengeResponses(challengeResponses)
                    .build();

            try {
                var response = cognitoClient.respondToAuthChallenge(challengeRequest);
                log.info("MFA challenge response successful for: {}", email);

                // Log successful MFA verification
                userActivityLogService.logActivity(
                    "MFA_VERIFY",
                    email,
                    appConfig.getUserPoolId(),
                    appConfig.getAppName(),
                    "SUCCESS",
                    "Challenge: " + challengeName,
                    request
                );

                return response;
            } catch (CognitoIdentityProviderException ex) {
                String errorMessage = ex.getMessage();
                if (errorMessage.contains("(Service:")) {
                    errorMessage = errorMessage.substring(0, errorMessage.indexOf("(Service:"));
                }

                // Log failed MFA verification
                userActivityLogService.logActivity(
                    "MFA_VERIFY",
                    email,
                    appConfig.getUserPoolId(),
                    appConfig.getAppName(),
                    "FAILURE",
                    errorMessage,
                    request
                );

                throw ex;
            }
        } finally {
            cognitoClient.close();
        }
    }

    /**
     * Resend confirmation code to user's email.
     *
     * @param email The user's email
     * @return The result of the resend confirmation code request
     */
    public ResendConfirmationCodeResponse resendConfirmationCode(String email) {
        log.info("Resending confirmation code for user with email: {}", email);
        CognitoAppConfig appConfig = getCurrentAppConfig();
        log.debug("Using app config: {}", appConfig.getAppName());
        CognitoIdentityProviderClient cognitoClient = createCognitoClient();

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

        try {
            ResendConfirmationCodeRequest resendRequest = ResendConfirmationCodeRequest.builder()
                    .clientId(appConfig.getClientId())
                    .username(email)
                    .build();

            try {
                var response = cognitoClient.resendConfirmationCode(resendRequest);
                log.info("Confirmation code resent successfully for: {}", email);

                // Log successful OTP resend
                userActivityLogService.logActivity(
                    "RESEND_OTP",
                    email,
                    appConfig.getUserPoolId(),
                    appConfig.getAppName(),
                    "SUCCESS",
                    null,
                    request
                );

                return response;
            } catch (CognitoIdentityProviderException ex) {
                String errorMessage = ex.getMessage();
                if (errorMessage.contains("(Service:")) {
                    errorMessage = errorMessage.substring(0, errorMessage.indexOf("(Service:"));
                }

                // Log failed OTP resend
                userActivityLogService.logActivity(
                    "RESEND_OTP",
                    email,
                    appConfig.getUserPoolId(),
                    appConfig.getAppName(),
                    "FAILURE",
                    errorMessage,
                    request
                );

                throw ex;
            }
        } finally {
            cognitoClient.close();
        }
    }

    /**
     * Introspect an access token to validate it.
     *
     * @param accessToken The access token to validate
     * @return GetUserResponse containing user information if token is valid
     * @throws NotAuthorizedException if the token is invalid or expired
     * @throws IllegalStateException if the Cognito configuration is not found
     */
    public GetUserResponse introspectToken(String accessToken) {
        log.info("Introspecting access token");
        CognitoAppConfig appConfig = getCurrentAppConfig();
        CognitoIdentityProviderClient cognitoClient = createCognitoClient();
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String username = null;

        try {
            GetUserRequest getUserRequest = GetUserRequest.builder()
                    .accessToken(accessToken)
                    .build();

            var response = cognitoClient.getUser(getUserRequest);
            username = response.username();

            log.info("Token validated successfully for user: {}", username);

            // Log successful introspection
            userActivityLogService.logActivity(
                    appConfig.getUserPoolId(),
                    username,
                    "TOKEN_INTROSPECT",
                    appConfig.getAppName(),
                    "SUCCESS",
                    null,
                    request
            );

            return response;

        } catch (NotAuthorizedException ex) {
            log.error("Token validation failed - invalid or expired token", ex);

            String errorMessage = ex.getMessage();
            if (errorMessage != null && errorMessage.contains("(Service:")) {
                errorMessage = errorMessage.substring(0, errorMessage.indexOf("(Service:")).trim();
            }

            // Log failed introspection
            userActivityLogService.logActivity(
                    appConfig.getUserPoolId(),
                    username != null ? username : "UNKNOWN",
                    "TOKEN_INTROSPECT",
                    appConfig.getAppName(),
                    "FAILURE",
                    errorMessage,
                    request
            );

            throw ex;

        } catch (Exception ex) {
            log.error("Error during token introspection", ex);

            String errorMessage = ex.getMessage();
            if (errorMessage != null && errorMessage.contains("(Service:")) {
                errorMessage = errorMessage.substring(0, errorMessage.indexOf("(Service:")).trim();
            }

            // Log failed introspection
            userActivityLogService.logActivity(
                    appConfig.getUserPoolId(),
                    username != null ? username : "UNKNOWN",
                    "TOKEN_INTROSPECT",
                    appConfig.getAppName(),
                    "FAILURE",
                    errorMessage,
                    request
            );

            throw ex;

        } finally {
            cognitoClient.close();
        }
    }
}