package com.oldmutual.AwsCognitoMiddleware.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for confirming forgot password.
 */
@Data
public class ConfirmForgotPasswordDto {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    @NotBlank(message = "Confirmation code is required")
    private String confirmationCode;
    
    @NotBlank(message = "New password is required")
    private String newPassword;
}