// 사용자 서비스 - 사용자 정보 조회, 수정, 비밀번호 변경 등 비즈니스 로직
package com.festapp.dashboard.user.service;

import com.festapp.dashboard.user.dto.UserCreateRequest;
import com.festapp.dashboard.user.dto.UserInfoResponse;
import com.festapp.dashboard.user.dto.UserUpdateRequest;
import com.festapp.dashboard.user.entity.User;
import com.festapp.dashboard.user.repository.UserRepository;
import com.festapp.dashboard.auth.repository.RefreshTokenRepository;
import com.festapp.dashboard.dashboard.service.DashboardProvisioningService;
import com.festapp.dashboard.common.exception.ResourceNotFoundException;
import com.festapp.dashboard.common.exception.ValidationException;
import com.festapp.dashboard.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final DashboardProvisioningService dashboardProvisioningService;

    /**
     * 공통 메서드: userId로 User 엔티티 조회
     */
    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with userId: {}", userId);
                    return new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND);
                });
    }

    @Transactional
    public UserInfoResponse createUser(UserCreateRequest createRequest) {
        log.info("Creating new user with username: {}", createRequest.getUsername());

        // 중복 검사
        if (userRepository.existsByUsername(createRequest.getUsername())) {
            log.warn("Username already exists: {}", createRequest.getUsername());
            throw new ValidationException(ErrorCode.INVALID_INPUT, "이미 존재하는 사용자명입니다");
        }

        if (userRepository.existsByEmail(createRequest.getEmail())) {
            log.warn("Email already exists: {}", createRequest.getEmail());
            throw new ValidationException(ErrorCode.EMAIL_ALREADY_EXISTS, createRequest.getEmail());
        }

        // 역할 검증
        User.Role role;
        try {
            role = User.Role.valueOf(createRequest.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid role: {}", createRequest.getRole());
            throw new ValidationException(ErrorCode.INVALID_INPUT, "유효하지 않은 역할입니다");
        }

        // 사용자 생성
        User newUser = User.builder()
                .username(createRequest.getUsername())
                .email(createRequest.getEmail())
                .password(passwordEncoder.encode(createRequest.getPassword()))
                .fullName(createRequest.getFullName())
                .role(role)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(newUser);
        dashboardProvisioningService.ensureDefaultDashboard(savedUser);
        log.info("User created successfully with userId: {}", savedUser.getUserId());
        return convertToDTO(savedUser);
    }

    public UserInfoResponse getUserById(Long userId) {
        log.debug("Fetching user by userId: {}", userId);
        User user = getUserOrThrow(userId);
        return convertToDTO(user);
    }

    public UserInfoResponse getUserByUsername(String username) {
        log.debug("Fetching user by username: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found with username: {}", username);
                    return new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND);
                });
        return convertToDTO(user);
    }

    public List<UserInfoResponse> getAllUsers() {
        log.debug("Fetching all users");
        return userRepository.findAll().stream()
                .map(this::convertToDTO)
                .toList();
    }

    public Page<UserInfoResponse> getAllUsersPageable(Pageable pageable) {
        log.debug("Fetching all users with pagination");
        return userRepository.findAllActiveUsersPageable(pageable)
                .map(this::convertToDTO);
    }

    public List<UserInfoResponse> searchUsers(String keyword) {
        log.debug("Searching users with keyword: {}", keyword);
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllUsers();
        }
        return userRepository.searchActiveUsers(keyword).stream()
                .map(this::convertToDTO)
                .toList();
    }

    public Page<UserInfoResponse> searchUsersPageable(String keyword, Pageable pageable) {
        log.debug("Searching users with keyword: {} and pagination", keyword);
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllUsersPageable(pageable);
        }
        return userRepository.searchActiveUsersPageable(keyword, pageable)
                .map(this::convertToDTO);
    }

    @Transactional
    public UserInfoResponse updateUser(Long userId, UserUpdateRequest updateRequest) {
        log.info("Updating user with userId: {}", userId);
        User user = getUserOrThrow(userId);

        // 이메일 변경 시 중복 검사
        if (updateRequest.getEmail() != null && !updateRequest.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(updateRequest.getEmail())) {
                log.warn("Email already exists: {}", updateRequest.getEmail());
                throw new ValidationException(ErrorCode.EMAIL_ALREADY_EXISTS, updateRequest.getEmail());
            }
            user.setEmail(updateRequest.getEmail());
            log.debug("Updated email for userId: {}", userId);
        }

        // 사용자명은 변경 불가 (보안상 이유)
        if (updateRequest.getFullName() != null && !updateRequest.getFullName().isEmpty()) {
            user.setFullName(updateRequest.getFullName());
            log.debug("Updated fullName for userId: {}", userId);
        }

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully with userId: {}", userId);
        return convertToDTO(updatedUser);
    }

    @Transactional
    public void activateUser(Long userId) {
        log.info("Activating user with userId: {}", userId);
        User user = getUserOrThrow(userId);
        
        if (user.getIsActive()) {
            log.warn("User is already active: {}", userId);
            throw new ValidationException(ErrorCode.DUPLICATE_ACTIVATION);
        }
        
        user.setIsActive(true);
        userRepository.save(user);
        log.info("User activated successfully with userId: {}", userId);
    }

    @Transactional
    public void deactivateUser(Long userId) {
        log.info("Deactivating user with userId: {}", userId);
        User user = getUserOrThrow(userId);
        
        if (!user.getIsActive()) {
            log.warn("User is already inactive: {}", userId);
            throw new ValidationException(ErrorCode.DUPLICATE_DEACTIVATION);
        }
        
        user.setIsActive(false);
        refreshTokenRepository.deleteByUser(user);
        userRepository.save(user);
        log.info("User deactivated successfully with userId: {}", userId);
    }

    @Transactional
    public void promoteToAdmin(Long userId) {
        log.info("Promoting user to ADMIN with userId: {}", userId);
        User user = getUserOrThrow(userId);
        
        if (user.getRole() == User.Role.ADMIN) {
            log.warn("User is already an admin: {}", userId);
            throw new ValidationException(ErrorCode.INVALID_ROLE_CHANGE, "이미 관리자입니다");
        }
        
        user.setRole(User.Role.ADMIN);
        userRepository.save(user);
        log.info("User promoted to ADMIN successfully with userId: {}", userId);
    }

    @Transactional
    public void demoteToUser(Long userId) {
        log.info("Demoting user to USER role with userId: {}", userId);
        User user = getUserOrThrow(userId);
        
        if (user.getRole() == User.Role.USER) {
            log.warn("User is already a regular user: {}", userId);
            throw new ValidationException(ErrorCode.INVALID_ROLE_CHANGE, "이미 일반 사용자입니다");
        }
        
        user.setRole(User.Role.USER);
        userRepository.save(user);
        log.info("User demoted to USER role successfully with userId: {}", userId);
    }

    @Transactional
    public void deleteUser(Long userId) {
        log.info("Deleting user with userId: {}", userId);
        User user = getUserOrThrow(userId);

        // Delete all refresh tokens associated with this user
        refreshTokenRepository.deleteByUser(user);

        // Delete the user
        userRepository.delete(user);
        log.info("User deleted successfully with userId: {}", userId);
    }

    public long getUserCountByRole(User.Role role) {
        log.debug("Counting users by role: {}", role);
        return userRepository.countByRole(role);
    }

    public long getActiveUserCount() {
        log.debug("Counting active users");
        return userRepository.countByIsActive(true);
    }

    public long getInactiveUserCount() {
        log.debug("Counting inactive users");
        return userRepository.countByIsActive(false);
    }

    /**
     * User 엔티티를 UserInfoResponse DTO로 변환
     */
    private UserInfoResponse convertToDTO(User user) {
        return UserInfoResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().toString())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}




