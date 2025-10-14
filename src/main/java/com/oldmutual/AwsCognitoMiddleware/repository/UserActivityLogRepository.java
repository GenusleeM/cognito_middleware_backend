package com.oldmutual.AwsCognitoMiddleware.repository;

import com.oldmutual.AwsCognitoMiddleware.model.UserActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for UserActivityLog entity.
 * Provides methods to interact with the user_activity_log table.
 */
@Repository
public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, Long> {

    /**
     * Find all activity logs for a specific user pool ordered by creation date descending.
     *
     * @param userPoolId the user pool ID
     * @return list of activity logs
     */
    List<UserActivityLog> findByUserPoolIdOrderByCreatedAtDesc(String userPoolId);

    /**
     * Find all activity logs for a specific username ordered by creation date descending.
     *
     * @param username the username
     * @return list of activity logs
     */
    List<UserActivityLog> findByUsernameOrderByCreatedAtDesc(String username);

    /**
     * Find all activity logs for a specific username in a specific user pool ordered by creation date descending.
     *
     * @param username the username
     * @param userPoolId the user pool ID
     * @return list of activity logs
     */
    List<UserActivityLog> findByUsernameAndUserPoolIdOrderByCreatedAtDesc(String username, String userPoolId);

    /**
     * Find all activity logs for a specific app name ordered by creation date descending.
     *
     * @param appName the app name
     * @return list of activity logs
     */
    List<UserActivityLog> findByAppNameOrderByCreatedAtDesc(String appName);

    /**
     * Find all activity logs created between the specified dates ordered by creation date descending.
     *
     * @param startDate the start date
     * @param endDate the end date
     * @return list of activity logs
     */
    List<UserActivityLog> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startDate, LocalDateTime endDate);

    // Pagination methods with filtering

    /**
     * Find logs by username containing (case-insensitive) with pagination.
     */
    Page<UserActivityLog> findByUsernameContainingIgnoreCase(String username, Pageable pageable);

    /**
     * Find logs by app name containing (case-insensitive) with pagination.
     */
    Page<UserActivityLog> findByAppNameContainingIgnoreCase(String appName, Pageable pageable);

    /**
     * Find logs by status with pagination.
     */
    Page<UserActivityLog> findByStatus(String status, Pageable pageable);

    /**
     * Find logs by activity containing (case-insensitive) with pagination.
     */
    Page<UserActivityLog> findByActivityContainingIgnoreCase(String activity, Pageable pageable);

    /**
     * Find logs by username and app name containing (case-insensitive) with pagination.
     */
    Page<UserActivityLog> findByUsernameContainingIgnoreCaseAndAppNameContainingIgnoreCase(String username, String appName, Pageable pageable);

    /**
     * Find logs by username containing and status with pagination.
     */
    Page<UserActivityLog> findByUsernameContainingIgnoreCaseAndStatus(String username, String status, Pageable pageable);

    /**
     * Find logs by username and activity containing (case-insensitive) with pagination.
     */
    Page<UserActivityLog> findByUsernameContainingIgnoreCaseAndActivityContainingIgnoreCase(String username, String activity, Pageable pageable);

    /**
     * Find logs by app name containing and status with pagination.
     */
    Page<UserActivityLog> findByAppNameContainingIgnoreCaseAndStatus(String appName, String status, Pageable pageable);

    /**
     * Find logs by app name and activity containing (case-insensitive) with pagination.
     */
    Page<UserActivityLog> findByAppNameContainingIgnoreCaseAndActivityContainingIgnoreCase(String appName, String activity, Pageable pageable);

    /**
     * Find logs by status and activity containing (case-insensitive) with pagination.
     */
    Page<UserActivityLog> findByStatusAndActivityContainingIgnoreCase(String status, String activity, Pageable pageable);
}