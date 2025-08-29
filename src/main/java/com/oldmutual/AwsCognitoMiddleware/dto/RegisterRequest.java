package com.oldmutual.AwsCognitoMiddleware.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Request DTO for user registration.
 * Supports custom attributes that vary per application.
 */
@Data
public class RegisterRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    @NotBlank(message = "Password is required")
    private String password;
    
    /**
     * Custom attributes for the user.
     * Examples include:
     * - name
     * - phone_number
     * - custom:National_ID
     * - custom:passport_number
     * - custom:dob
     * - custom:nationality
     * - custom:country_of_origin
     * - custom:verification_status
     */
    private Map<String, String> attributes = new HashMap<>();
}