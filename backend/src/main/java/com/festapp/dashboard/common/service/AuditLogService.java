// Audit 로깅 서비스
package com.festapp.dashboard.common.service;

import com.festapp.dashboard.common.entity.AuditLog;
import com.festapp.dashboard.common.repository.AuditLogRepository;
import com.festapp.dashboard.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Audit 로깅 서비스
 *
 * 사용자의 중요한 행동들을 로깅합니다.
 *
 * @author DashBoar Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void logAction(User user, String actionType, String entityType, Long entityId,
                         String description, String requestUrl, String ipAddress,
                         String status, String errorMessage) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .user(user)
                    .actionType(actionType)
                    .entityType(entityType)
                    .entityId(entityId)
                    .description(description)
                    .requestUrl(requestUrl)
                    .ipAddress(ipAddress)
                    .status(status)
                    .errorMessage(errorMessage)
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log saved: user={}, action={}, entity={}", user.getUsername(), actionType, entityType);
        } catch (Exception e) {
            log.error("Failed to save audit log", e);
            // 로깅 실패가 메인 로직을 방해하지 않도록
        }
    }

    @Transactional
    public void logLoginSuccess(User user, String ipAddress, String requestUrl) {
        logAction(user, "LOGIN", "USER", user.getUserId(),
                 "User logged in successfully", requestUrl, ipAddress, "SUCCESS", null);
    }

    @Transactional
    public void logLoginFailure(String username, String ipAddress, String requestUrl, String reason) {
        logAction(null, "LOGIN_FAILED", "USER", null,
                 "Login failed for user: " + username, requestUrl, ipAddress, "FAILURE", reason);
    }

    @Transactional
    public void logLogout(User user, String ipAddress, String requestUrl) {
        logAction(user, "LOGOUT", "USER", user.getUserId(),
                 "User logged out", requestUrl, ipAddress, "SUCCESS", null);
    }

    @Transactional
    public void logUserCreated(User performedBy, Long createdUserId, String createdUsername) {
        logAction(performedBy, "CREATE", "USER", createdUserId,
                 "User created: " + createdUsername, null, null, "SUCCESS", null);
    }

    @Transactional
    public void logUserUpdated(User performedBy, Long updatedUserId) {
        logAction(performedBy, "UPDATE", "USER", updatedUserId,
                 "User information updated", null, null, "SUCCESS", null);
    }

    @Transactional
    public void logUserDeleted(User performedBy, Long deletedUserId) {
        logAction(performedBy, "DELETE", "USER", deletedUserId,
                 "User deleted", null, null, "SUCCESS", null);
    }
}

