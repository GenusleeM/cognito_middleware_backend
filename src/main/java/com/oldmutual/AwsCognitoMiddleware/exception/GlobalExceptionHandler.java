package com.oldmutual.AwsCognitoMiddleware.exception;

import com.oldmutual.AwsCognitoMiddleware.dto.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the application.
 * Provides standardized error responses for common exceptions.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle validation errors from @Valid annotations.
     *
     * @param ex The validation exception
     * @return A response with validation error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.error("Validation error: {}", errors);
        return ResponseEntity.badRequest().body(ApiResponse.error("Validation failed", errors));
    }

    /**
     * Handle constraint violation exceptions.
     *
     * @param ex The constraint violation exception
     * @return A response with validation error details
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(ConstraintViolationException ex) {
        log.error("Constraint violation: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error("Validation failed: " + ex.getMessage()));
    }

    /**
     * Handle missing request header exceptions.
     *
     * @param ex The missing header exception
     * @return A response indicating the missing header
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingRequestHeaderException(MissingRequestHeaderException ex) {
        log.error("Missing header: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Missing required header: " + ex.getHeaderName()));
    }

    /**
     * Handle Cognito service exceptions.
     *
     * @param ex The Cognito exception
     * @return A response with the Cognito error details
     */
    @ExceptionHandler(CognitoIdentityProviderException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleCognitoException(CognitoIdentityProviderException ex) {
        String fullErrorMessage = ex.getMessage();
        log.error("Cognito error: {}", fullErrorMessage);
        
        // Extract the core error message without AWS service details
        String cleanErrorMessage = extractCleanErrorMessage(fullErrorMessage);
        
        Map<String, String> errorData = new HashMap<>();
        errorData.put("errorType", ex.getClass().getSimpleName());
        errorData.put("errorMessage", cleanErrorMessage);
        
        return ResponseEntity.badRequest().body(ApiResponse.error("Invalid credentials", errorData));
    }
    
    /**
     * Extracts the clean error message from the full AWS error message.
     * Removes the AWS service details in parentheses.
     *
     * @param fullErrorMessage The full error message from AWS
     * @return The clean error message without AWS service details
     */
    private String extractCleanErrorMessage(String fullErrorMessage) {
        if (fullErrorMessage == null) {
            return "Unknown error";
        }
        
        // Extract the message before the AWS service details
        int serviceIndex = fullErrorMessage.indexOf(" (Service:");
        if (serviceIndex > 0) {
            return fullErrorMessage.substring(0, serviceIndex);
        }
        
        return fullErrorMessage;
    }

    /**
     * Handle general exceptions.
     *
     * @param ex The exception
     * @return A generic error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred"));
    }
}