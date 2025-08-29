package com.oldmutual.AwsCognitoMiddleware.service;

import com.oldmutual.AwsCognitoMiddleware.dto.AppConfigRequest;
import com.oldmutual.AwsCognitoMiddleware.dto.AppConfigResponse;
import com.oldmutual.AwsCognitoMiddleware.model.CognitoAppConfig;
import com.oldmutual.AwsCognitoMiddleware.repository.CognitoAppConfigRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for admin operations related to app configuration management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final CognitoAppConfigRepository appConfigRepository;

    /**
     * Get all app configurations.
     *
     * @return List of all app configurations
     */
    @Transactional(readOnly = true)
    public List<AppConfigResponse> getAllApps() {
        return appConfigRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get an app configuration by ID.
     *
     * @param id The app configuration ID
     * @return The app configuration
     * @throws EntityNotFoundException if the app configuration is not found
     */
    @Transactional(readOnly = true)
    public AppConfigResponse getAppById(Long id) {
        CognitoAppConfig appConfig = appConfigRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("App configuration not found with ID: " + id));
        return mapToResponse(appConfig);
    }

    /**
     * Create a new app configuration.
     *
     * @param request The app configuration request
     * @return The created app configuration
     */
    @Transactional
    public AppConfigResponse createApp(AppConfigRequest request) {
        CognitoAppConfig appConfig = new CognitoAppConfig();
        appConfig.setAppKey(UUID.randomUUID());
        appConfig.setEnabled(true);
        updateAppFromRequest(appConfig, request);
        
        CognitoAppConfig savedConfig = appConfigRepository.save(appConfig);
        log.info("Created new app configuration with ID: {} and app key: {}", savedConfig.getId(), savedConfig.getAppKey());
        
        return mapToResponse(savedConfig);
    }

    /**
     * Update an existing app configuration.
     *
     * @param id      The app configuration ID
     * @param request The updated app configuration request
     * @return The updated app configuration
     * @throws EntityNotFoundException if the app configuration is not found
     */
    @Transactional
    public AppConfigResponse updateApp(Long id, AppConfigRequest request) {
        CognitoAppConfig appConfig = appConfigRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("App configuration not found with ID: " + id));
        
        updateAppFromRequest(appConfig, request);
        CognitoAppConfig updatedConfig = appConfigRepository.save(appConfig);
        log.info("Updated app configuration with ID: {}", updatedConfig.getId());
        
        return mapToResponse(updatedConfig);
    }

    /**
     * Enable an app.
     *
     * @param id The app configuration ID
     * @throws EntityNotFoundException if the app configuration is not found
     */
    @Transactional
    public void enableApp(Long id) {
        CognitoAppConfig appConfig = appConfigRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("App configuration not found with ID: " + id));
        
        appConfig.setEnabled(true);
        appConfigRepository.save(appConfig);
        log.info("Enabled app configuration with ID: {}", id);
    }

    /**
     * Disable an app.
     *
     * @param id The app configuration ID
     * @throws EntityNotFoundException if the app configuration is not found
     */
    @Transactional
    public void disableApp(Long id) {
        CognitoAppConfig appConfig = appConfigRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("App configuration not found with ID: " + id));
        
        appConfig.setEnabled(false);
        appConfigRepository.save(appConfig);
        log.info("Disabled app configuration with ID: {}", id);
    }

    /**
     * Delete an app configuration.
     *
     * @param id The app configuration ID
     * @throws EntityNotFoundException if the app configuration is not found
     */
    @Transactional
    public void deleteApp(Long id) {
        if (!appConfigRepository.existsById(id)) {
            throw new EntityNotFoundException("App configuration not found with ID: " + id);
        }
        
        appConfigRepository.deleteById(id);
        log.info("Deleted app configuration with ID: {}", id);
    }

    /**
     * Update an app configuration from a request.
     *
     * @param appConfig The app configuration to update
     * @param request   The request with updated values
     */
    private void updateAppFromRequest(CognitoAppConfig appConfig, AppConfigRequest request) {
        appConfig.setAppName(request.getAppName());
        appConfig.setAwsRegion(request.getAwsRegion());
        appConfig.setUserPoolId(request.getUserPoolId());
        appConfig.setClientId(request.getClientId());
        appConfig.setClientSecret(request.getClientSecret());
    }

    /**
     * Map a CognitoAppConfig entity to an AppConfigResponse DTO.
     *
     * @param appConfig The app configuration entity
     * @return The app configuration response DTO
     */
    private AppConfigResponse mapToResponse(CognitoAppConfig appConfig) {
        return new AppConfigResponse(
                appConfig.getId(),
                appConfig.getAppKey(),
                appConfig.getAppName(),
                appConfig.getAwsRegion(),
                appConfig.getUserPoolId(),
                appConfig.getClientId(),
                appConfig.isEnabled()
        );
    }
}