// Audit 로깅 리포지토리
package com.festapp.dashboard.common.repository;

import com.festapp.dashboard.common.entity.AuditLog;
import com.festapp.dashboard.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT al FROM AuditLog al WHERE al.user = :user ORDER BY al.createdAt DESC")
    Page<AuditLog> findByUserOrderByCreatedAtDesc(@Param("user") User user, Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE al.actionType = :actionType ORDER BY al.createdAt DESC")
    List<AuditLog> findByActionType(@Param("actionType") String actionType);

    @Query("SELECT al FROM AuditLog al WHERE al.createdAt BETWEEN :startDate AND :endDate ORDER BY al.createdAt DESC")
    List<AuditLog> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT al FROM AuditLog al WHERE al.status = :status ORDER BY al.createdAt DESC")
    List<AuditLog> findByStatus(@Param("status") String status);

    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.user = :user")
    long countByUser(@Param("user") User user);
}

