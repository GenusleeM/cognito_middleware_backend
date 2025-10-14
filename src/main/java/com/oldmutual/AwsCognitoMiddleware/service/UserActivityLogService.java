package com.oldmutual.AwsCognitoMiddleware.service;

import com.oldmutual.AwsCognitoMiddleware.model.UserActivityLog;
import com.oldmutual.AwsCognitoMiddleware.repository.UserActivityLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing user activity logs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserActivityLogService {

    private final UserActivityLogRepository userActivityLogRepository;
    
    /**
     * Log a user activity.
     *
     * @param activity the activity description
     * @param username the username
     * @param userPoolId the user pool ID
     * @param appName the application name
     * @param status the status of the activity (success/failure)
     * @param errorMessage the error message if any
     * @param request the HTTP request
     * @return the created log entry
     */
    public UserActivityLog logActivity(
            String activity,
            String username,
            String userPoolId,
            String appName,
            String status,
            String errorMessage,
            HttpServletRequest request
    ) {
        String ipAddress = extractIpAddress(request);
        
        UserActivityLog activityLog = UserActivityLog.builder()
                .activity(activity)
                .username(username)
                .userPoolId(userPoolId)
                .appName(appName)
                .status(status)
                .errorMessage(errorMessage)
                .ipAddress(ipAddress)
                .createdAt(LocalDateTime.now())
                .build();
        
        log.debug("Logging user activity: {}", activityLog);
        return userActivityLogRepository.save(activityLog);
    }
    
    /**
     * Get all activity logs ordered by creation date descending.
     *
     * @return list of all activity logs
     */
    public List<UserActivityLog> getAllLogs() {
        return userActivityLogRepository.findAll(org.springframework.data.domain.Sort.by(
            org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }

    /**
     * Get activity logs for a specific user pool ordered by creation date descending.
     *
     * @param userPoolId the user pool ID
     * @return list of activity logs
     */
    public List<UserActivityLog> getLogsByUserPool(String userPoolId) {
        return userActivityLogRepository.findByUserPoolIdOrderByCreatedAtDesc(userPoolId);
    }

    /**
     * Get activity logs for a specific username ordered by creation date descending.
     *
     * @param username the username
     * @return list of activity logs
     */
    public List<UserActivityLog> getLogsByUsername(String username) {
        return userActivityLogRepository.findByUsernameOrderByCreatedAtDesc(username);
    }

    /**
     * Get activity logs for a specific username in a specific user pool ordered by creation date descending.
     *
     * @param username the username
     * @param userPoolId the user pool ID
     * @return list of activity logs
     */
    public List<UserActivityLog> getLogsByUsernameAndUserPool(String username, String userPoolId) {
        return userActivityLogRepository.findByUsernameAndUserPoolIdOrderByCreatedAtDesc(username, userPoolId);
    }

    /**
     * Get activity logs for a specific app name ordered by creation date descending.
     *
     * @param appName the app name
     * @return list of activity logs
     */
    public List<UserActivityLog> getLogsByAppName(String appName) {
        return userActivityLogRepository.findByAppNameOrderByCreatedAtDesc(appName);
    }

    /**
     * Get activity logs created between the specified dates ordered by creation date descending.
     *
     * @param startDate the start date
     * @param endDate the end date
     * @return list of activity logs
     */
    public List<UserActivityLog> getLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return userActivityLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startDate, endDate);
    }

    /**
     * Get all activity logs with pagination and optional filtering.
     *
     * @param page the page number (0-indexed)
     * @param size the page size
     * @param username optional username filter
     * @param appName optional app name filter
     * @param status optional status filter (SUCCESS/FAILURE)
     * @param activity optional activity type filter
     * @return paginated activity logs
     */
    public Page<UserActivityLog> getLogsPaginated(int page, int size, String username, String appName, String status, String activity) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Apply filters
        if (username != null && !username.isEmpty()) {
            if (appName != null && !appName.isEmpty()) {
                return userActivityLogRepository.findByUsernameContainingIgnoreCaseAndAppNameContainingIgnoreCase(username, appName, pageable);
            } else if (status != null && !status.isEmpty()) {
                return userActivityLogRepository.findByUsernameContainingIgnoreCaseAndStatus(username, status, pageable);
            } else if (activity != null && !activity.isEmpty()) {
                return userActivityLogRepository.findByUsernameContainingIgnoreCaseAndActivityContainingIgnoreCase(username, activity, pageable);
            }
            return userActivityLogRepository.findByUsernameContainingIgnoreCase(username, pageable);
        } else if (appName != null && !appName.isEmpty()) {
            if (status != null && !status.isEmpty()) {
                return userActivityLogRepository.findByAppNameContainingIgnoreCaseAndStatus(appName, status, pageable);
            } else if (activity != null && !activity.isEmpty()) {
                return userActivityLogRepository.findByAppNameContainingIgnoreCaseAndActivityContainingIgnoreCase(appName, activity, pageable);
            }
            return userActivityLogRepository.findByAppNameContainingIgnoreCase(appName, pageable);
        } else if (status != null && !status.isEmpty()) {
            if (activity != null && !activity.isEmpty()) {
                return userActivityLogRepository.findByStatusAndActivityContainingIgnoreCase(status, activity, pageable);
            }
            return userActivityLogRepository.findByStatus(status, pageable);
        } else if (activity != null && !activity.isEmpty()) {
            return userActivityLogRepository.findByActivityContainingIgnoreCase(activity, pageable);
        }

        return userActivityLogRepository.findAll(pageable);
    }

    /**
     * Extract the client IP address from the request.
     *
     * @param request the HTTP request
     * @return the client IP address
     */
    private String extractIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }
}