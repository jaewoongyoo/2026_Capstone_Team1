// Audit 로깅 엔티티 - 사용자 행동 추적
package com.festapp.dashboard.common.entity;

import com.festapp.dashboard.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 감사 로그 엔티티
 *
 * 사용자의 중요한 행동들을 기록합니다.
 * - 로그인/로그아웃
 * - 데이터 생성/수정/삭제
 * - 권한 변경 등
 *
 * @author DashBoar Team
 * @version 1.0.0
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_action", columnList = "action_type")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "user")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType; // LOGIN, LOGOUT, CREATE, UPDATE, DELETE, etc.

    @Column(name = "entity_type", length = 50)
    private String entityType; // USER, WIDGET, etc.

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "request_url")
    private String requestUrl;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "status", length = 20)
    private String status; // SUCCESS, FAILURE

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

