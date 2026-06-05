// 사용자 리포지토리 - 사용자 데이터 접근 계층
package com.festapp.dashboard.user.repository;

import com.festapp.dashboard.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.isActive = true")
    List<User> findAllActiveUsers();

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.isActive = true")
    List<User> findByRoleAndActive(@Param("role") User.Role role);

    // 페이징 관련 쿼리
    @Query("SELECT u FROM User u WHERE u.isActive = true")
    Page<User> findAllActiveUsersPageable(Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.isActive = true")
    Page<User> findByRoleAndActivePageable(@Param("role") User.Role role, Pageable pageable);

    // 검색 관련 쿼리
    @Query("SELECT u FROM User u WHERE (LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND u.isActive = true")
    List<User> searchActiveUsers(@Param("keyword") String keyword);

    @Query("SELECT u FROM User u WHERE (LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND u.isActive = true")
    Page<User> searchActiveUsersPageable(@Param("keyword") String keyword, Pageable pageable);

    List<User> findByRoleOrderByCreatedAtDesc(User.Role role);

    long countByRole(User.Role role);

    long countByIsActive(Boolean isActive);
}

