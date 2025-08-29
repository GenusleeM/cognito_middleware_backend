package com.oldmutual.AwsCognitoMiddleware.repository;

import com.oldmutual.AwsCognitoMiddleware.model.CognitoAppConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CognitoAppConfig entity.
 * Provides methods to find configuration by appKey.
 */
@Repository
public interface CognitoAppConfigRepository extends JpaRepository<CognitoAppConfig, Long> {
    
    /**
     * Find a configuration by its appKey (UUID).
     * 
     * @param appKey The UUID of the application
     * @return Optional containing the configuration if found, empty otherwise
     */
    Optional<CognitoAppConfig> findByAppKey(UUID appKey);
}