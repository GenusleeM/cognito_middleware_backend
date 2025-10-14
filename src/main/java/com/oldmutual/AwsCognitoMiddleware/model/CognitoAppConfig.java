package com.oldmutual.AwsCognitoMiddleware.model;

import com.oldmutual.AwsCognitoMiddleware.config.AttributeEncryptor;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * JPA Entity representing Cognito application configuration.
 * Maps to the app_config table in the database.
 */
@Entity
@Table(name = "app_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CognitoAppConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "UUID", unique = true, nullable = false)
    private UUID appKey;

    @Column(nullable = false)
    private String appName;

    @Column(nullable = false)
    private String awsRegion;

    @Column(nullable = false)
    private String userPoolId;

    @Column(nullable = false)
    private String clientId;

    /**
     * Client secret is stored encrypted in the database.
     * Jasypt is used for encryption/decryption.
     * This field is optional as some Cognito operations only require userPoolId and clientId.
     */

    @Convert(converter = AttributeEncryptor.class)
    private String clientSecret;
    
    /**
     * Flag to indicate if the application is enabled.
     * Disabled applications will be rejected by the interceptor.
     */
    @Column(nullable = false)
    private boolean enabled = true;
}