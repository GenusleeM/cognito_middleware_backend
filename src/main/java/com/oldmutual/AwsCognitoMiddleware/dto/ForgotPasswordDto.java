package com.oldmutual.AwsCognitoMiddleware.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for forgot password.
 */
@Data
public class ForgotPasswordDto {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
}