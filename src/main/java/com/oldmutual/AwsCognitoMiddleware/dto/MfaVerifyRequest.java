package com.oldmutual.AwsCognitoMiddleware.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for MFA verification during login.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MfaVerifyRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Session is required")
    private String session;

    @NotBlank(message = "MFA code is required")
    private String mfaCode;

    @NotBlank(message = "Challenge name is required")
    private String challengeName; // SMS_MFA, SOFTWARE_TOKEN_MFA, or EMAIL_OTP
}
