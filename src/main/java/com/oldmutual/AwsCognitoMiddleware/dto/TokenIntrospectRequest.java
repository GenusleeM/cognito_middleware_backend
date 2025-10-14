package com.oldmutual.AwsCognitoMiddleware.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for token introspection.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenIntrospectRequest {

    @NotBlank(message = "Access token is required")
    private String accessToken;
}
