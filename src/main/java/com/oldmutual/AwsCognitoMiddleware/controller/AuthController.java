package com.oldmutual.AwsCognitoMiddleware.controller;

import com.oldmutual.AwsCognitoMiddleware.dto.ApiResponse;
import com.oldmutual.AwsCognitoMiddleware.dto.ConfirmForgotPasswordDto;
import com.oldmutual.AwsCognitoMiddleware.dto.ForgotPasswordDto;
import com.oldmutual.AwsCognitoMiddleware.dto.LoginRequest;
import com.oldmutual.AwsCognitoMiddleware.dto.RegisterRequest;
import com.oldmutual.AwsCognitoMiddleware.dto.VerifyRequest;
import com.oldmutual.AwsCognitoMiddleware.dto.MfaSetupRequest;
import com.oldmutual.AwsCognitoMiddleware.dto.MfaVerifyRequest;
import com.oldmutual.AwsCognitoMiddleware.dto.AssociateSoftwareTokenRequest;
import com.oldmutual.AwsCognitoMiddleware.dto.VerifySoftwareTokenRequest;
import com.oldmutual.AwsCognitoMiddleware.dto.ResendOtpRequest;
import com.oldmutual.AwsCognitoMiddleware.dto.TokenIntrospectRequest;
import com.oldmutual.AwsCognitoMiddleware.service.CognitoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CodeMismatchException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmSignUpResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ForgotPasswordResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmForgotPasswordResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UsernameExistsException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ChallengeNameType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ResendConfirmationCodeResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ExpiredCodeException;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for authentication endpoints.
 * All endpoints require the X-APP-KEY header to be present.
 * 
 * @author Genuslee Mapedze
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CognitoService cognitoService;

    /**
     * Helper method to clean error messages by removing AWS service details.
     *
     * @param errorMessage The original error message
     * @return Cleaned error message without AWS service details
     */
    private String cleanErrorMessage(String errorMessage) {
        if (errorMessage != null && errorMessage.contains("(Service:")) {
            return errorMessage.substring(0, errorMessage.indexOf("(Service:")).trim();
        }
        return errorMessage;
    }

    /**
     * Register a new user.
     *
     * @param request The registration request
     * @return The registration response
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Received registration request for email: {}", request.getEmail());
        try {
            // Pass email, password, and custom attributes to the service
            SignUpResponse response = cognitoService.registerUser(
                request.getEmail(), 
                request.getPassword(),
                request.getAttributes()
            );
            
            Map<String, Object> data = new HashMap<>();
            data.put("username", response.userSub());
            data.put("status", "UNCONFIRMED"); // User needs to verify email
            data.put("userConfirmationNecessary", "true");

            return ResponseEntity.ok(ApiResponse.success(data, "User registered successfully. Please check your email for verification code"));
        } catch (UsernameExistsException e) {
            log.error("Username already exists: {}", request.getEmail(), e);
            String cleanMessage = cleanErrorMessage(e.getMessage());
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "UsernameExistsException");
            errorData.put("errorMessage", cleanMessage);
            return ResponseEntity.badRequest().body(ApiResponse.error("User with this email already exists", errorData));
        } catch (IllegalStateException e) {
            log.error("Configuration error: {}", e.getMessage(), e);
            String cleanMessage = cleanErrorMessage(e.getMessage());
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "ConfigurationError");
            errorData.put("errorMessage", cleanMessage);
            return ResponseEntity.badRequest().body(ApiResponse.error("Configuration error: " + cleanMessage, errorData));
        } catch (software.amazon.awssdk.core.exception.SdkClientException e) {
            log.error("AWS SDK client error: {}", e.getMessage(), e);
            String cleanMessage = cleanErrorMessage(e.getMessage());
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "AwsCredentialsError");
            errorData.put("errorMessage", cleanMessage);
            return ResponseEntity.badRequest().body(ApiResponse.error("AWS credentials error. Please contact support.", errorData));
        } catch (Exception e) {
            log.error("Error registering user: {}", request.getEmail(), e);
            String cleanMessage = cleanErrorMessage(e.getMessage());
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "UnexpectedException");
            errorData.put("errorMessage", cleanMessage);
            return ResponseEntity.badRequest().body(ApiResponse.error("Error registering user: " + cleanMessage, errorData));
        }
    }

    /**
     * Verify a user's account with a confirmation code.
     *
     * @param request The verification request
     * @return The verification response
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verify(@Valid @RequestBody VerifyRequest request) {
        log.info("Received verification request for email: {}", request.getEmail());
        try {
            ConfirmSignUpResponse response = cognitoService.verifyUser(request.getEmail(), request.getConfirmationCode());

            Map<String, Object> data = new HashMap<>();
            data.put("status", "CONFIRMED");

            return ResponseEntity.ok(ApiResponse.success(data, "User verified successfully"));
        } catch (ExpiredCodeException e) {
            log.error("Expired confirmation code for user: {}", request.getEmail(), e);
            String cleanMessage = cleanErrorMessage(e.getMessage());
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "ExpiredCodeException");
            errorData.put("errorMessage", "Your verification code has expired");
            errorData.put("action", "Please use the resend OTP endpoint to get a new code");
            errorData.put("resendEndpoint", "/api/auth/resend-otp");
            return ResponseEntity.badRequest().body(ApiResponse.error("Verification code has expired. Please request a new code.", errorData));
        } catch (CodeMismatchException e) {
            log.error("Invalid confirmation code for user: {}", request.getEmail(), e);
            String cleanMessage = cleanErrorMessage(e.getMessage());
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "CodeMismatchException");
            errorData.put("errorMessage", "The verification code you entered is incorrect");
            errorData.put("action", "Please check the code and try again, or request a new code");
            errorData.put("resendEndpoint", "/api/auth/resend-otp");
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid confirmation code. Please check and try again.", errorData));
        } catch (NotAuthorizedException e) {
            log.error("User already verified or not authorized: {}", request.getEmail(), e);
            String cleanMessage = cleanErrorMessage(e.getMessage());
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "NotAuthorizedException");
            errorData.put("errorMessage", "User is already verified or cannot be verified");
            errorData.put("action", "Try logging in - your account may already be verified");
            return ResponseEntity.badRequest().body(ApiResponse.error("User is already verified or cannot be verified", errorData));
        } catch (IllegalStateException e) {
            log.error("Configuration error: {}", e.getMessage(), e);
            String cleanMessage = cleanErrorMessage(e.getMessage());
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "ConfigurationError");
            errorData.put("errorMessage", cleanMessage);
            return ResponseEntity.badRequest().body(ApiResponse.error("Configuration error: " + cleanMessage, errorData));
        } catch (software.amazon.awssdk.core.exception.SdkClientException e) {
            log.error("AWS SDK client error: {}", e.getMessage(), e);
            String cleanMessage = cleanErrorMessage(e.getMessage());
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "AwsCredentialsError");
            errorData.put("errorMessage", cleanMessage);
            return ResponseEntity.badRequest().body(ApiResponse.error("AWS credentials error. Please contact support.", errorData));
        } catch (Exception e) {
            log.error("Error verifying user: {}", request.getEmail(), e);
            String cleanMessage = cleanErrorMessage(e.getMessage());
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "UnexpectedException");
            errorData.put("errorMessage", cleanMessage);
            return ResponseEntity.badRequest().body(ApiResponse.error("Error verifying user: " + cleanMessage, errorData));
        }
    }

    /**
     * Resend OTP/confirmation code to user's email.
     *
     * @param request The resend OTP request
     * @return The resend OTP response
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        log.info("Received resend OTP request for email: {}", request.getEmail());
        try {
            ResendConfirmationCodeResponse response = cognitoService.resendConfirmationCode(request.getEmail());

            Map<String, Object> data = new HashMap<>();
            if (response.codeDeliveryDetails() != null) {
                data.put("deliveryMedium", response.codeDeliveryDetails().deliveryMediumAsString());
                data.put("destination", response.codeDeliveryDetails().destination());
            }
            data.put("message", "A new verification code has been sent to your email");

            return ResponseEntity.ok(ApiResponse.success(data, "Confirmation code resent successfully to email"));
        } catch (NotAuthorizedException e) {
            log.error("User already verified or not authorized to resend code: {}", request.getEmail(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "NotAuthorizedException");
            errorData.put("errorMessage", "User is already verified");
            errorData.put("action", "Your account is already verified. You can log in directly.");
            return ResponseEntity.badRequest().body(ApiResponse.error("User is already verified. Please log in.", errorData));
        } catch (UserNotFoundException e) {
            log.error("User not found: {}", request.getEmail(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "UserNotFoundException");
            errorData.put("errorMessage", "No user found with this email address");
            errorData.put("action", "Please check your email address or register a new account");
            return ResponseEntity.badRequest().body(ApiResponse.error("User not found", errorData));
        } catch (IllegalStateException e) {
            log.error("Configuration error: {}", e.getMessage(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "ConfigurationError");
            String cleanMsg = cleanErrorMessage(e.getMessage());
            errorData.put("errorMessage", cleanMsg);
            return ResponseEntity.badRequest().body(ApiResponse.error("Configuration error: " + e.getMessage(), errorData));
        } catch (software.amazon.awssdk.core.exception.SdkClientException e) {
            log.error("AWS SDK client error: {}", e.getMessage(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "AwsCredentialsError");
            String cleanMsg = cleanErrorMessage(e.getMessage());
            errorData.put("errorMessage", cleanMsg);
            return ResponseEntity.badRequest().body(ApiResponse.error("AWS credentials error. Please contact support.", errorData));
        } catch (Exception e) {
            log.error("Error resending OTP for user: {}", request.getEmail(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "UnexpectedException");
            String cleanMsg = cleanErrorMessage(e.getMessage());
            errorData.put("errorMessage", cleanMsg);
            return ResponseEntity.badRequest().body(ApiResponse.error("Error resending OTP: " + e.getMessage(), errorData));
        }
    }

    /**
     * Authenticate a user.
     * If MFA is enabled, returns a challenge that requires MFA verification.
     *
     * @param request The login request
     * @return The authentication response with tokens or MFA challenge
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Received login request for email: {}", request.getEmail());
        try {
            InitiateAuthResponse response = cognitoService.authenticateUser(request.getEmail(), request.getPassword());

            Map<String, Object> data = new HashMap<>();

            // Check if MFA challenge is required
            if (response.challengeName() != null && !response.challengeName().equals(ChallengeNameType.UNKNOWN_TO_SDK_VERSION)) {
                // MFA challenge required
                data.put("challengeName", response.challengeNameAsString());
                data.put("session", response.session());
                data.put("challengeParameters", response.challengeParameters());

                return ResponseEntity.ok(ApiResponse.success(data, "MFA verification required. Please provide the MFA code."));
            }

            // No MFA challenge - return tokens
            AuthenticationResultType authResult = response.authenticationResult();

            data.put("accessToken", authResult.accessToken());
            data.put("refreshToken", authResult.refreshToken());
            data.put("idToken", authResult.idToken());
            data.put("expiresIn", String.valueOf(authResult.expiresIn()));

            return ResponseEntity.ok(ApiResponse.success(data, "Login successful"));
        } catch (NotAuthorizedException e) {
            log.error("Invalid credentials for user: {}", request.getEmail(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "NotAuthorizedException");
            String cleanMsg = cleanErrorMessage(e.getMessage());
            errorData.put("errorMessage", cleanMsg);
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid credentials", errorData));
        } catch (UserNotFoundException e) {
            log.error("User not found: {}", request.getEmail(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "UserNotFoundException");
            String cleanMsg = cleanErrorMessage(e.getMessage());
            errorData.put("errorMessage", cleanMsg);
            return ResponseEntity.badRequest().body(ApiResponse.error("User not found", errorData));
        } catch (IllegalStateException e) {
            log.error("Configuration error: {}", e.getMessage(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "ConfigurationError");
            String cleanMsg = cleanErrorMessage(e.getMessage());
            errorData.put("errorMessage", cleanMsg);
            return ResponseEntity.badRequest().body(ApiResponse.error("Configuration error: " + e.getMessage(), errorData));
        } catch (software.amazon.awssdk.core.exception.SdkClientException e) {
            log.error("AWS SDK client error: {}", e.getMessage(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "AwsCredentialsError");
            String cleanMsg = cleanErrorMessage(e.getMessage());
            errorData.put("errorMessage", cleanMsg);
            return ResponseEntity.badRequest().body(ApiResponse.error("AWS credentials error. Please contact support.", errorData));
        } catch (Exception e) {
            log.error("Error authenticating user: {}", request.getEmail(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "UnexpectedException");
            String cleanMsg = cleanErrorMessage(e.getMessage());
            errorData.put("errorMessage", cleanMsg);
            return ResponseEntity.badRequest().body(ApiResponse.error("Error authenticating user: " + e.getMessage(), errorData));
        }
    }

    /**
     * Initiate the forgot password flow.
     *
     * @param request The forgot password request
     * @return The forgot password response
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Map<String, Object>>> forgotPassword(@Valid @RequestBody ForgotPasswordDto request) {
        log.info("Received forgot password request for email: {}", request.getEmail());
        try {
            ForgotPasswordResponse response = cognitoService.forgotPassword(request.getEmail());
            
            Map<String, Object> data = new HashMap<>();
            if (response.codeDeliveryDetails() != null) {
                data.put("deliveryMedium", response.codeDeliveryDetails().deliveryMediumAsString());
                data.put("destination", response.codeDeliveryDetails().destination());
            }

            return ResponseEntity.ok(ApiResponse.success(data, "Password reset code sent to email"));
        } catch (UserNotFoundException e) {
            log.error("User not found: {}", request.getEmail(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "UserNotFoundException");
            String cleanMsg = cleanErrorMessage(e.getMessage());
            errorData.put("errorMessage", cleanMsg);
            return ResponseEntity.badRequest().body(ApiResponse.error("User not found", errorData));
        } catch (IllegalStateException e) {
            log.error("Configuration error: {}", e.getMessage(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "ConfigurationError");
            String cleanMsg = cleanErrorMessage(e.getMessage());
            errorData.put("errorMessage", cleanMsg);
            return ResponseEntity.badRequest().body(ApiResponse.error("Configuration error: " + e.getMessage(), errorData));
        } catch (software.amazon.awssdk.core.exception.SdkClientException e) {
            log.error("AWS SDK client error: {}", e.getMessage(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "AwsCredentialsError");
            String cleanMsg = cleanErrorMessage(e.getMessage());
            errorData.put("errorMessage", cleanMsg);
            return ResponseEntity.badRequest().body(ApiResponse.error("AWS credentials error. Please contact support.", errorData));
        } catch (Exception e) {
            log.error("Error initiating forgot password: {}", request.getEmail(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "UnexpectedException");
            String cleanMsg = cleanErrorMessage(e.getMessage());
            errorData.put("errorMessage", cleanMsg);
            return ResponseEntity.badRequest().body(ApiResponse.error("Error initiating forgot password: " + e.getMessage(), errorData));
        }
    }

    /**
     * Confirm a new password with the confirmation code.
     *
     * @param request The confirm forgot password request
     * @return The confirm forgot password response
     */
    @PostMapping("/confirm-forgot-password")
    public ResponseEntity<ApiResponse<Map<String, Object>>> confirmForgotPassword(@Valid @RequestBody ConfirmForgotPasswordDto request) {
        log.info("Received confirm forgot password request for email: {}", request.getEmail());
        try {
            ConfirmForgotPasswordResponse response = cognitoService.confirmForgotPassword(
                request.getEmail(),
                request.getConfirmationCode(),
                request.getNewPassword()
            );

            Map<String, Object> data = new HashMap<>();

            return ResponseEntity.ok(ApiResponse.success(data, "Password reset successful"));
        } catch (CodeMismatchException e) {
            log.error("Invalid confirmation code for user: {}", request.getEmail(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "CodeMismatchException");
            String cleanMsg = cleanErrorMessage(e.getMessage());
            errorData.put("errorMessage", cleanMsg);
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid confirmation code", errorData));
        } catch (UserNotFoundException e) {
            log.error("User not found: {}", request.getEmail(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "UserNotFoundException");
            String cleanMsg = cleanErrorMessage(e.getMessage());
            errorData.put("errorMessage", cleanMsg);
            return ResponseEntity.badRequest().body(ApiResponse.error("User not found", errorData));
        } catch (IllegalStateException e) {
            log.error("Configuration error: {}", e.getMessage(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "ConfigurationError");
            String cleanMsg = cleanErrorMessage(e.getMessage());
            errorData.put("errorMessage", cleanMsg);
            return ResponseEntity.badRequest().body(ApiResponse.error("Configuration error: " + e.getMessage(), errorData));
        } catch (software.amazon.awssdk.core.exception.SdkClientException e) {
            log.error("AWS SDK client error: {}", e.getMessage(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "AwsCredentialsError");
            String cleanMsg = cleanErrorMessage(e.getMessage());
            errorData.put("errorMessage", cleanMsg);
            return ResponseEntity.badRequest().body(ApiResponse.error("AWS credentials error. Please contact support.", errorData));
        } catch (Exception e) {
            log.error("Error confirming forgot password: {}", request.getEmail(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "UnexpectedException");
            String cleanMsg = cleanErrorMessage(e.getMessage());
            errorData.put("errorMessage", cleanMsg);
            return ResponseEntity.badRequest().body(ApiResponse.error("Error confirming forgot password: " + e.getMessage(), errorData));
        }
    }

    /**
     * Setup MFA for a user (SMS or Software Token).
     *
     * @param request The MFA setup request
     * @return The MFA setup response
     */
    @PostMapping("/mfa/setup")
    public ResponseEntity<ApiResponse<Map<String, Object>>> setupMfa(@Valid @RequestBody MfaSetupRequest request) {
        log.info("Received MFA setup request for type: {}", request.getMfaType());
        try {
            cognitoService.setUserMfaPreference(
                request.getAccessToken(),
                request.getMfaType(),
                request.getPhoneNumber()
            );

            Map<String, Object> data = new HashMap<>();
            data.put("mfaType", request.getMfaType());
            data.put("status", "MFA_ENABLED");

            return ResponseEntity.ok(ApiResponse.success(data, "MFA setup successful"));
        } catch (IllegalStateException e) {
            log.error("Configuration error: {}", e.getMessage(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "ConfigurationError");
            String cleanMsg = cleanErrorMessage(e.getMessage());
            errorData.put("errorMessage", cleanMsg);
            return ResponseEntity.badRequest().body(ApiResponse.error("Configuration error: " + e.getMessage(), errorData));
        } catch (Exception e) {
            log.error("Error setting up MFA", e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "UnexpectedException");
            String cleanMsg = cleanErrorMessage(e.getMessage());
            errorData.put("errorMessage", cleanMsg);
            return ResponseEntity.badRequest().body(ApiResponse.error("Error setting up MFA: " + e.getMessage(), errorData));
        }
    }

    /**
     * Associate a software token (TOTP) for MFA.
     *
     * @param request The associate software token request
     * @return The associate software token response with secret code
     */
    @PostMapping("/mfa/associate-token")
    public ResponseEntity<ApiResponse<Map<String, Object>>> associateSoftwareToken(@Valid @RequestBody AssociateSoftwareTokenRequest request) {
        log.info("Received associate software token request");
        try {
            var response = cognitoService.associateSoftwareToken(request.getAccessToken());

            Map<String, Object> data = new HashMap<>();
            data.put("secretCode", response.secretCode());
            data.put("session", response.session());

            return ResponseEntity.ok(ApiResponse.success(data, "Software token associated. Use the secret code with your authenticator app"));
        } catch (IllegalStateException e) {
            log.error("Configuration error: {}", e.getMessage(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "ConfigurationError");
            String cleanMsg = cleanErrorMessage(e.getMessage());
            errorData.put("errorMessage", cleanMsg);
            return ResponseEntity.badRequest().body(ApiResponse.error("Configuration error: " + e.getMessage(), errorData));
        } catch (Exception e) {
            log.error("Error associating software token", e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "UnexpectedException");
            String cleanMsg = cleanErrorMessage(e.getMessage());
            errorData.put("errorMessage", cleanMsg);
            return ResponseEntity.badRequest().body(ApiResponse.error("Error associating software token: " + e.getMessage(), errorData));
        }
    }

    /**
     * Verify a software token (TOTP) for MFA setup.
     *
     * @param request The verify software token request
     * @return The verify software token response
     */
    @PostMapping("/mfa/verify-token")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifySoftwareToken(@Valid @RequestBody VerifySoftwareTokenRequest request) {
        log.info("Received verify software token request");
        try {
            var response = cognitoService.verifySoftwareToken(
                request.getAccessToken(),
                request.getUserCode(),
                request.getDeviceName()
            );

            Map<String, Object> data = new HashMap<>();
            data.put("status", response.statusAsString());

            return ResponseEntity.ok(ApiResponse.success(data, "Software token verified successfully"));
        } catch (CodeMismatchException e) {
            log.error("Invalid verification code", e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "CodeMismatchException");
            String cleanMsg = cleanErrorMessage(e.getMessage());
            errorData.put("errorMessage", cleanMsg);
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid verification code", errorData));
        } catch (IllegalStateException e) {
            log.error("Configuration error: {}", e.getMessage(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "ConfigurationError");
            String cleanMsg = cleanErrorMessage(e.getMessage());
            errorData.put("errorMessage", cleanMsg);
            return ResponseEntity.badRequest().body(ApiResponse.error("Configuration error: " + e.getMessage(), errorData));
        } catch (Exception e) {
            log.error("Error verifying software token", e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "UnexpectedException");
            String cleanMsg = cleanErrorMessage(e.getMessage());
            errorData.put("errorMessage", cleanMsg);
            return ResponseEntity.badRequest().body(ApiResponse.error("Error verifying software token: " + e.getMessage(), errorData));
        }
    }

    /**
     * Verify MFA code during login.
     *
     * @param request The MFA verification request
     * @return The authentication response with tokens
     */
    @PostMapping("/mfa/verify")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyMfa(@Valid @RequestBody MfaVerifyRequest request) {
        log.info("Received MFA verification request for email: {}", request.getEmail());
        try {
            var response = cognitoService.respondToMfaChallenge(
                request.getEmail(),
                request.getSession(),
                request.getMfaCode(),
                request.getChallengeName()
            );

            Map<String, Object> data = new HashMap<>();
            AuthenticationResultType authResult = response.authenticationResult();

            data.put("accessToken", authResult.accessToken());
            data.put("refreshToken", authResult.refreshToken());
            data.put("idToken", authResult.idToken());
            data.put("expiresIn", String.valueOf(authResult.expiresIn()));

            return ResponseEntity.ok(ApiResponse.success(data, "MFA verification successful"));
        } catch (CodeMismatchException e) {
            log.error("Invalid MFA code for user: {}", request.getEmail(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "CodeMismatchException");
            String cleanMsg = cleanErrorMessage(e.getMessage());
            errorData.put("errorMessage", cleanMsg);
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid MFA code", errorData));
        } catch (IllegalStateException e) {
            log.error("Configuration error: {}", e.getMessage(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "ConfigurationError");
            String cleanMsg = cleanErrorMessage(e.getMessage());
            errorData.put("errorMessage", cleanMsg);
            return ResponseEntity.badRequest().body(ApiResponse.error("Configuration error: " + e.getMessage(), errorData));
        } catch (Exception e) {
            log.error("Error verifying MFA for user: {}", request.getEmail(), e);

            // Clean error message - remove AWS service details
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("(Service:")) {
                errorMessage = errorMessage.substring(0, errorMessage.indexOf("(Service:")).trim();
            }

            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "UnexpectedException");
            errorData.put("errorMessage", errorMessage);
            return ResponseEntity.badRequest().body(ApiResponse.error("Error verifying MFA: " + errorMessage, errorData));
        }
    }

    /**
     * Introspect an access token to validate it.
     *
     * @param request The token introspection request
     * @return The token validation response with user information
     */
    @PostMapping("/token/introspect")
    public ResponseEntity<ApiResponse<Map<String, Object>>> introspectToken(@Valid @RequestBody TokenIntrospectRequest request) {
        log.info("Received token introspection request");
        try {
            var response = cognitoService.introspectToken(request.getAccessToken());

            Map<String, Object> data = new HashMap<>();
            data.put("valid", true);
            data.put("username", response.username());
            data.put("email", response.userAttributes().stream()
                    .filter(attr -> "email".equals(attr.name()))
                    .findFirst()
                    .map(attr -> attr.value())
                    .orElse(null));

            // Add other user attributes
            Map<String, String> attributes = new HashMap<>();
            response.userAttributes().forEach(attr -> attributes.put(attr.name(), attr.value()));
            data.put("attributes", attributes);

            return ResponseEntity.ok(ApiResponse.success(data, "Token is valid"));
        } catch (NotAuthorizedException e) {
            log.error("Invalid or expired token", e);
            String cleanMessage = cleanErrorMessage(e.getMessage());
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("valid", false);
            errorData.put("errorType", "NotAuthorizedException");
            errorData.put("errorMessage", cleanMessage);
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid or expired token", errorData));
        } catch (IllegalStateException e) {
            log.error("Configuration error: {}", e.getMessage(), e);
            String cleanMessage = cleanErrorMessage(e.getMessage());
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("valid", false);
            errorData.put("errorType", "ConfigurationError");
            errorData.put("errorMessage", cleanMessage);
            return ResponseEntity.badRequest().body(ApiResponse.error("Configuration error: " + cleanMessage, errorData));
        } catch (Exception e) {
            log.error("Error introspecting token", e);
            String cleanMessage = cleanErrorMessage(e.getMessage());
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("valid", false);
            errorData.put("errorType", "UnexpectedException");
            errorData.put("errorMessage", cleanMessage);
            return ResponseEntity.badRequest().body(ApiResponse.error("Error introspecting token: " + cleanMessage, errorData));
        }
    }
}