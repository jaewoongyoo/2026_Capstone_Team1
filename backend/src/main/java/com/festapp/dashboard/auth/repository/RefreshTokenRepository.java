// 리프레시 토큰 리포지토리 - 토큰 데이터 접근 계층
package com.festapp.dashboard.auth.repository;

import com.festapp.dashboard.auth.entity.RefreshToken;
import com.festapp.dashboard.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    void deleteByUser(User user);

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user = :user AND rt.isRevoked = false")
    List<RefreshToken> findActiveTokensByUser(@Param("user") User user);

    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :currentTime")
    void deleteExpiredTokens(@Param("currentTime") LocalDateTime currentTime);

    // 추가 쿼리 메서드
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user = :user AND rt.isRevoked = false AND rt.expiresAt > :currentTime")
    List<RefreshToken> findValidTokensByUser(@Param("user") User user, @Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user = :user AND rt.isRevoked = false AND rt.expiresAt > :currentTime")
    long countActiveTokensByUser(@Param("user") User user, @Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user = :user AND rt.expiresAt < :currentTime")
    long countExpiredTokensByUser(@Param("user") User user, @Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user = :user AND rt.isRevoked = true")
    long countRevokedTokensByUser(@Param("user") User user);

    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user = :user")
    long countAllTokensByUser(@Param("user") User user);

    @Query("SELECT MAX(rt.createdAt) FROM RefreshToken rt WHERE rt.user = :user")
    LocalDateTime findLastTokenCreatedAtByUser(@Param("user") User user);
}



