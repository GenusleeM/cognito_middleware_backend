package com.oldmutual.AwsCognitoMiddleware.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for verifying software token (TOTP) setup.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerifySoftwareTokenRequest {

    @NotBlank(message = "Access token is required")
    private String accessToken;

    @NotBlank(message = "User code is required")
    private String userCode;

    @NotBlank(message = "Device friendly name is required")
    private String deviceName;
}
