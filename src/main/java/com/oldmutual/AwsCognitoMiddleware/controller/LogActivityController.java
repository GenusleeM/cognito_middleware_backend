package com.oldmutual.AwsCognitoMiddleware.controller;

import com.oldmutual.AwsCognitoMiddleware.dto.ApiResponse;
import com.oldmutual.AwsCognitoMiddleware.model.UserActivityLog;
import com.oldmutual.AwsCognitoMiddleware.service.UserActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for user activity log endpoints.
 */
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class LogActivityController {

    private final UserActivityLogService userActivityLogService;

    /**
     * Get all activity logs.
     *
     * @return list of all activity logs
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserActivityLog>>> getAllLogs() {
        log.info("Getting all activity logs");
        List<UserActivityLog> logs = userActivityLogService.getAllLogs();
        return ResponseEntity.ok(ApiResponse.success(logs, "Activity logs retrieved successfully"));
    }

    /**
     * Get activity logs for a specific user pool.
     *
     * @param userPoolId the user pool ID
     * @return list of activity logs
     */
    @GetMapping("/user-pool/{userPoolId}")
    public ResponseEntity<ApiResponse<List<UserActivityLog>>> getLogsByUserPool(@PathVariable String userPoolId) {
        log.info("Getting activity logs for user pool: {}", userPoolId);
        List<UserActivityLog> logs = userActivityLogService.getLogsByUserPool(userPoolId);
        return ResponseEntity.ok(ApiResponse.success(logs, "Activity logs retrieved successfully"));
    }

    /**
     * Get activity logs for a specific username.
     *
     * @param username the username
     * @return list of activity logs
     */
    @GetMapping("/user/{username}")
    public ResponseEntity<ApiResponse<List<UserActivityLog>>> getLogsByUsername(@PathVariable String username) {
        log.info("Getting activity logs for username: {}", username);
        List<UserActivityLog> logs = userActivityLogService.getLogsByUsername(username);
        return ResponseEntity.ok(ApiResponse.success(logs, "Activity logs retrieved successfully"));
    }

    /**
     * Get activity logs for a specific username in a specific user pool.
     *
     * @param username the username
     * @param userPoolId the user pool ID
     * @return list of activity logs
     */
    @GetMapping("/user/{username}/user-pool/{userPoolId}")
    public ResponseEntity<ApiResponse<List<UserActivityLog>>> getLogsByUsernameAndUserPool(
            @PathVariable String username,
            @PathVariable String userPoolId) {
        log.info("Getting activity logs for username: {} in user pool: {}", username, userPoolId);
        List<UserActivityLog> logs = userActivityLogService.getLogsByUsernameAndUserPool(username, userPoolId);
        return ResponseEntity.ok(ApiResponse.success(logs, "Activity logs retrieved successfully"));
    }

    /**
     * Get activity logs for a specific app name.
     *
     * @param appName the app name
     * @return list of activity logs
     */
    @GetMapping("/app/{appName}")
    public ResponseEntity<ApiResponse<List<UserActivityLog>>> getLogsByAppName(@PathVariable String appName) {
        log.info("Getting activity logs for app: {}", appName);
        List<UserActivityLog> logs = userActivityLogService.getLogsByAppName(appName);
        return ResponseEntity.ok(ApiResponse.success(logs, "Activity logs retrieved successfully"));
    }

    /**
     * Get activity logs created between the specified dates.
     *
     * @param startDate the start date
     * @param endDate the end date
     * @return list of activity logs
     */
    @GetMapping("/date-range")
    public ResponseEntity<ApiResponse<List<UserActivityLog>>> getLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("Getting activity logs between {} and {}", startDate, endDate);
        List<UserActivityLog> logs = userActivityLogService.getLogsByDateRange(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(logs, "Activity logs retrieved successfully"));
    }

    /**
     * Get activity logs with pagination and optional filters.
     *
     * @param page page number (0-indexed, default: 0)
     * @param size page size (default: 20)
     * @param username optional username filter
     * @param appName optional app name filter
     * @param status optional status filter (SUCCESS/FAILURE)
     * @param activity optional activity type filter
     * @return paginated activity logs with metadata
     */
    @GetMapping("/paginated")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLogsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String appName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String activity) {

        log.info("Getting paginated logs - page: {}, size: {}, filters: [username={}, appName={}, status={}, activity={}]",
                page, size, username, appName, status, activity);

        Page<UserActivityLog> logsPage = userActivityLogService.getLogsPaginated(page, size, username, appName, status, activity);

        Map<String, Object> response = new HashMap<>();
        response.put("logs", logsPage.getContent());
        response.put("currentPage", logsPage.getNumber());
        response.put("totalPages", logsPage.getTotalPages());
        response.put("totalElements", logsPage.getTotalElements());
        response.put("pageSize", logsPage.getSize());
        response.put("hasNext", logsPage.hasNext());
        response.put("hasPrevious", logsPage.hasPrevious());

        return ResponseEntity.ok(ApiResponse.success(response, "Activity logs retrieved successfully"));
    }
}