package com.oldmutual.AwsCognitoMiddleware.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for changing password (when user is authenticated).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

    @NotBlank(message = "Access token is required")
    private String accessToken;

    @NotBlank(message = "Previous password is required")
    private String previousPassword;

    @NotBlank(message = "Proposed password is required")
    private String proposedPassword;
}
