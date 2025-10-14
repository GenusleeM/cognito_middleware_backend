package com.oldmutual.AwsCognitoMiddleware.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for MFA setup.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MfaSetupRequest {

    @NotBlank(message = "Access token is required")
    private String accessToken;

    @NotBlank(message = "MFA type is required (SMS_MFA or SOFTWARE_TOKEN_MFA)")
    private String mfaType; // SMS_MFA or SOFTWARE_TOKEN_MFA (for email, use SMS_MFA with email attribute)

    private String phoneNumber; // Required for SMS_MFA
}
