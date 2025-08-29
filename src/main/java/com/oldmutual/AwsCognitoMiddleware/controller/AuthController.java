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
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CodeMismatchException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmSignUpResponse;
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
    public ResponseEntity<ApiResponse<Map<String, String>>> register(@Valid @RequestBody RegisterRequest request) {
        try {
            // Pass email, password, and custom attributes to the service
            AdminCreateUserResponse response = cognitoService.registerUser(
                request.getEmail(), 
                request.getPassword(),
                request.getAttributes()
            );
            
            Map<String, String> data = new HashMap<>();
            data.put("username", response.user().username());
            data.put("status", response.user().userStatusAsString());
            
            return ResponseEntity.ok(ApiResponse.success(data, "User registered successfully"));
        } catch (UsernameExistsException e) {
            log.error("Username already exists: {}", request.getEmail(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("User with this email already exists"));
        } catch (IllegalStateException e) {
            log.error("Configuration error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Configuration error: " + e.getMessage()));
        } catch (software.amazon.awssdk.core.exception.SdkClientException e) {
            log.error("AWS SDK client error: {}", e.getMessage(), e);
            Map<String, String> errorDetails = new HashMap<>();
            errorDetails.put("errorType", "AwsCredentialsError");
            errorDetails.put("errorMessage", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("AWS credentials error. Please contact support.", errorDetails));
        } catch (Exception e) {
            log.error("Error registering user: {}", request.getEmail(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Error registering user: " + e.getMessage()));
        }
    }

    /**
     * Verify a user's account with a confirmation code.
     *
     * @param request The verification request
     * @return The verification response
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verify(@Valid @RequestBody VerifyRequest request) {
        try {
            cognitoService.verifyUser(request.getEmail(), request.getConfirmationCode());
            return ResponseEntity.ok(ApiResponse.success("User verified successfully"));
        } catch (CodeMismatchException e) {
            log.error("Invalid confirmation code for user: {}", request.getEmail(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid confirmation code"));
        } catch (IllegalStateException e) {
            log.error("Configuration error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Configuration error: " + e.getMessage()));
        } catch (software.amazon.awssdk.core.exception.SdkClientException e) {
            log.error("AWS SDK client error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("AWS credentials error. Please contact support."));
        } catch (Exception e) {
            log.error("Error verifying user: {}", request.getEmail(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Error verifying user: " + e.getMessage()));
        }
    }

    /**
     * Authenticate a user.
     *
     * @param request The login request
     * @return The authentication response with tokens
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> login(@Valid @RequestBody LoginRequest request) {
        try {
            AdminInitiateAuthResponse response = cognitoService.authenticateUser(request.getEmail(), request.getPassword());
            
            Map<String, String> tokens = new HashMap<>();
            AuthenticationResultType authResult = response.authenticationResult();
            
            tokens.put("accessToken", authResult.accessToken());
            tokens.put("refreshToken", authResult.refreshToken());
            tokens.put("idToken", authResult.idToken());
            tokens.put("expiresIn", String.valueOf(authResult.expiresIn()));
            
            return ResponseEntity.ok(ApiResponse.success(tokens, "Login successful"));
        } catch (NotAuthorizedException e) {
            log.error("Invalid credentials for user: {}", request.getEmail(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid credentials"));
        } catch (UserNotFoundException e) {
            log.error("User not found: {}", request.getEmail(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("User not found"));
        } catch (IllegalStateException e) {
            log.error("Configuration error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Configuration error: " + e.getMessage()));
        } catch (software.amazon.awssdk.core.exception.SdkClientException e) {
            log.error("AWS SDK client error: {}", e.getMessage(), e);
            Map<String, String> errorDetails = new HashMap<>();
            errorDetails.put("errorType", "AwsCredentialsError");
            errorDetails.put("errorMessage", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("AWS credentials error. Please contact support.", errorDetails));
        } catch (Exception e) {
            log.error("Error authenticating user: {}", request.getEmail(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Error authenticating user: " + e.getMessage()));
        }
    }

    /**
     * Initiate the forgot password flow.
     *
     * @param request The forgot password request
     * @return The forgot password response
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordDto request) {
        try {
            cognitoService.forgotPassword(request.getEmail());
            return ResponseEntity.ok(ApiResponse.success("Password reset code sent to email"));
        } catch (UserNotFoundException e) {
            log.error("User not found: {}", request.getEmail(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("User not found"));
        } catch (IllegalStateException e) {
            log.error("Configuration error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Configuration error: " + e.getMessage()));
        } catch (software.amazon.awssdk.core.exception.SdkClientException e) {
            log.error("AWS SDK client error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("AWS credentials error. Please contact support."));
        } catch (Exception e) {
            log.error("Error initiating forgot password: {}", request.getEmail(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Error initiating forgot password: " + e.getMessage()));
        }
    }

    /**
     * Confirm a new password with the confirmation code.
     *
     * @param request The confirm forgot password request
     * @return The confirm forgot password response
     */
    @PostMapping("/confirm-forgot-password")
    public ResponseEntity<ApiResponse<Void>> confirmForgotPassword(@Valid @RequestBody ConfirmForgotPasswordDto request) {
        try {
            cognitoService.confirmForgotPassword(request.getEmail(), request.getConfirmationCode(), request.getNewPassword());
            return ResponseEntity.ok(ApiResponse.success("Password reset successful"));
        } catch (CodeMismatchException e) {
            log.error("Invalid confirmation code for user: {}", request.getEmail(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid confirmation code"));
        } catch (UserNotFoundException e) {
            log.error("User not found: {}", request.getEmail(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("User not found"));
        } catch (IllegalStateException e) {
            log.error("Configuration error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Configuration error: " + e.getMessage()));
        } catch (software.amazon.awssdk.core.exception.SdkClientException e) {
            log.error("AWS SDK client error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("AWS credentials error. Please contact support."));
        } catch (Exception e) {
            log.error("Error confirming forgot password: {}", request.getEmail(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Error confirming forgot password: " + e.getMessage()));
        }
    }
}