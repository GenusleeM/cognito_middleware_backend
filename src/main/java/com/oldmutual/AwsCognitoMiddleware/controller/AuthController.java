package com.oldmutual.AwsCognitoMiddleware.controller;

import com.oldmutual.AwsCognitoMiddleware.dto.ApiResponse;
import com.oldmutual.AwsCognitoMiddleware.dto.ConfirmForgotPasswordDto;
import com.oldmutual.AwsCognitoMiddleware.dto.ForgotPasswordDto;
import com.oldmutual.AwsCognitoMiddleware.dto.LoginRequest;
import com.oldmutual.AwsCognitoMiddleware.dto.RegisterRequest;
import com.oldmutual.AwsCognitoMiddleware.dto.VerifyRequest;
import com.oldmutual.AwsCognitoMiddleware.service.CognitoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;
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

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for authentication endpoints.
 * All endpoints require the X-APP-KEY header to be present.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CognitoService cognitoService;

    /**
     * Register a new user.
     *
     * @param request The registration request
     * @return The registration response
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@Valid @RequestBody RegisterRequest request) {
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
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "UsernameExistsException");
            errorData.put("errorMessage", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("User with this email already exists", errorData));
        } catch (IllegalStateException e) {
            log.error("Configuration error: {}", e.getMessage(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "ConfigurationError");
            errorData.put("errorMessage", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Configuration error: " + e.getMessage(), errorData));
        } catch (software.amazon.awssdk.core.exception.SdkClientException e) {
            log.error("AWS SDK client error: {}", e.getMessage(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "AwsCredentialsError");
            errorData.put("errorMessage", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("AWS credentials error. Please contact support.", errorData));
        } catch (Exception e) {
            log.error("Error registering user: {}", request.getEmail(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "UnexpectedException");
            errorData.put("errorMessage", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Error registering user: " + e.getMessage(), errorData));
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
        try {
            ConfirmSignUpResponse response = cognitoService.verifyUser(request.getEmail(), request.getConfirmationCode());
            
            Map<String, Object> data = new HashMap<>();
            data.put("status", "CONFIRMED");

            return ResponseEntity.ok(ApiResponse.success(data, "User verified successfully"));
        } catch (CodeMismatchException e) {
            log.error("Invalid confirmation code for user: {}", request.getEmail(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "CodeMismatchException");
            errorData.put("errorMessage", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid confirmation code", errorData));
        } catch (IllegalStateException e) {
            log.error("Configuration error: {}", e.getMessage(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "ConfigurationError");
            errorData.put("errorMessage", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Configuration error: " + e.getMessage(), errorData));
        } catch (software.amazon.awssdk.core.exception.SdkClientException e) {
            log.error("AWS SDK client error: {}", e.getMessage(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "AwsCredentialsError");
            errorData.put("errorMessage", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("AWS credentials error. Please contact support.", errorData));
        } catch (Exception e) {
            log.error("Error verifying user: {}", request.getEmail(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "UnexpectedException");
            errorData.put("errorMessage", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Error verifying user: " + e.getMessage(), errorData));
        }
    }

    /**
     * Authenticate a user.
     *
     * @param request The login request
     * @return The authentication response with tokens
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@Valid @RequestBody LoginRequest request) {
        try {
            InitiateAuthResponse response = cognitoService.authenticateUser(request.getEmail(), request.getPassword());
            
            Map<String, Object> data = new HashMap<>();
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
            errorData.put("errorMessage", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid credentials", errorData));
        } catch (UserNotFoundException e) {
            log.error("User not found: {}", request.getEmail(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "UserNotFoundException");
            errorData.put("errorMessage", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("User not found", errorData));
        } catch (IllegalStateException e) {
            log.error("Configuration error: {}", e.getMessage(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "ConfigurationError");
            errorData.put("errorMessage", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Configuration error: " + e.getMessage(), errorData));
        } catch (software.amazon.awssdk.core.exception.SdkClientException e) {
            log.error("AWS SDK client error: {}", e.getMessage(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "AwsCredentialsError");
            errorData.put("errorMessage", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("AWS credentials error. Please contact support.", errorData));
        } catch (Exception e) {
            log.error("Error authenticating user: {}", request.getEmail(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "UnexpectedException");
            errorData.put("errorMessage", e.getMessage());
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
            errorData.put("errorMessage", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("User not found", errorData));
        } catch (IllegalStateException e) {
            log.error("Configuration error: {}", e.getMessage(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "ConfigurationError");
            errorData.put("errorMessage", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Configuration error: " + e.getMessage(), errorData));
        } catch (software.amazon.awssdk.core.exception.SdkClientException e) {
            log.error("AWS SDK client error: {}", e.getMessage(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "AwsCredentialsError");
            errorData.put("errorMessage", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("AWS credentials error. Please contact support.", errorData));
        } catch (Exception e) {
            log.error("Error initiating forgot password: {}", request.getEmail(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "UnexpectedException");
            errorData.put("errorMessage", e.getMessage());
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
            errorData.put("errorMessage", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid confirmation code", errorData));
        } catch (UserNotFoundException e) {
            log.error("User not found: {}", request.getEmail(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "UserNotFoundException");
            errorData.put("errorMessage", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("User not found", errorData));
        } catch (IllegalStateException e) {
            log.error("Configuration error: {}", e.getMessage(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "ConfigurationError");
            errorData.put("errorMessage", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Configuration error: " + e.getMessage(), errorData));
        } catch (software.amazon.awssdk.core.exception.SdkClientException e) {
            log.error("AWS SDK client error: {}", e.getMessage(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "AwsCredentialsError");
            errorData.put("errorMessage", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("AWS credentials error. Please contact support.", errorData));
        } catch (Exception e) {
            log.error("Error confirming forgot password: {}", request.getEmail(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", "UnexpectedException");
            errorData.put("errorMessage", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Error confirming forgot password: " + e.getMessage(), errorData));
        }
    }
}