package com.oldmutual.AwsCognitoMiddleware.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for associating software token (TOTP) for MFA.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssociateSoftwareTokenRequest {

    @NotBlank(message = "Access token is required")
    private String accessToken;
}
