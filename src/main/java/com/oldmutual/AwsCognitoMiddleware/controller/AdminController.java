package com.oldmutual.AwsCognitoMiddleware.controller;

import com.oldmutual.AwsCognitoMiddleware.dto.ApiResponse;
import com.oldmutual.AwsCognitoMiddleware.dto.AppConfigRequest;
import com.oldmutual.AwsCognitoMiddleware.dto.AppConfigResponse;
import com.oldmutual.AwsCognitoMiddleware.model.CognitoAppConfig;
import com.oldmutual.AwsCognitoMiddleware.repository.CognitoAppConfigRepository;
import com.oldmutual.AwsCognitoMiddleware.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for admin operations related to app configuration management.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/apps")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * Get all app configurations.
     *
     * @return List of all app configurations
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AppConfigResponse>>> getAllApps() {
        List<AppConfigResponse> apps = adminService.getAllApps();
        return ResponseEntity.ok(ApiResponse.success(apps, "App configurations retrieved successfully"));
    }

    /**
     * Get an app configuration by ID.
     *
     * @param id The app configuration ID
     * @return The app configuration
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AppConfigResponse>> getAppById(@PathVariable Long id) {
        AppConfigResponse app = adminService.getAppById(id);
        return ResponseEntity.ok(ApiResponse.success(app, "App configuration retrieved successfully"));
    }

    /**
     * Create a new app configuration.
     *
     * @param request The app configuration request
     * @return The created app configuration
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AppConfigResponse>> createApp(@Valid @RequestBody AppConfigRequest request) {
        AppConfigResponse createdApp = adminService.createApp(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdApp, "App configuration created successfully"));
    }

    /**
     * Update an existing app configuration.
     *
     * @param id      The app configuration ID
     * @param request The updated app configuration request
     * @return The updated app configuration
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AppConfigResponse>> updateApp(
            @PathVariable Long id,
            @Valid @RequestBody AppConfigRequest request) {
        AppConfigResponse updatedApp = adminService.updateApp(id, request);
        return ResponseEntity.ok(ApiResponse.success(updatedApp, "App configuration updated successfully"));
    }

    /**
     * Enable an app.
     *
     * @param id The app configuration ID
     * @return Success message
     */
    @PutMapping("/{id}/enable")
    public ResponseEntity<ApiResponse<Void>> enableApp(@PathVariable Long id) {
        adminService.enableApp(id);
        return ResponseEntity.ok(ApiResponse.success("App enabled successfully"));
    }

    /**
     * Disable an app.
     *
     * @param id The app configuration ID
     * @return Success message
     */
    @PutMapping("/{id}/disable")
    public ResponseEntity<ApiResponse<Void>> disableApp(@PathVariable Long id) {
        adminService.disableApp(id);
        return ResponseEntity.ok(ApiResponse.success("App disabled successfully"));
    }

    /**
     * Delete an app configuration.
     *
     * @param id The app configuration ID
     * @return Success message
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteApp(@PathVariable Long id) {
        adminService.deleteApp(id);
        return ResponseEntity.ok(ApiResponse.success("App configuration deleted successfully"));
    }
}