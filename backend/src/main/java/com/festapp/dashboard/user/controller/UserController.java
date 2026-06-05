// 사용자 제어자 - 사용자 정보 조회, 수정 API 처리
package com.festapp.dashboard.user.controller;

import com.festapp.dashboard.common.dto.ApiResponse;
import com.festapp.dashboard.user.dto.UserCreateRequest;
import com.festapp.dashboard.user.dto.UserInfoResponse;
import com.festapp.dashboard.user.dto.UserUpdateRequest;
import com.festapp.dashboard.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "사용자 관리 API")
public class UserController {

    private final UserService userService;

    @PostMapping
    @Operation(summary = "사용자 생성", description = "새로운 사용자를 생성합니다 (ADMIN 권한 필요)")
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserInfoResponse>> createUser(
            @Valid @RequestBody UserCreateRequest createRequest) {
        UserInfoResponse response = userService.createUser(createRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("사용자 생성 성공", response));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "사용자 정보 조회", description = "특정 사용자 정보 조회")
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("hasRole('ADMIN') or #userId == principal.userId")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getUserById(
            @PathVariable @NotNull(message = "userId는 필수입니다") Long userId) {
        UserInfoResponse response = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success("조회 성공", response));
    }

    @GetMapping("/username/{username}")
    @Operation(summary = "사용자명으로 조회", description = "사용자명으로 사용자 정보 조회")
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("hasRole('ADMIN') or #username == principal.username")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getUserByUsername(
            @PathVariable @NotNull(message = "username은 필수입니다") String username) {
        UserInfoResponse response = userService.getUserByUsername(username);
        return ResponseEntity.ok(ApiResponse.success("조회 성공", response));
    }

    @GetMapping
    @Operation(summary = "모든 사용자 조회", description = "모든 사용자 목록 조회 (ADMIN 권한 필요)")
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserInfoResponse>>> getAllUsers() {
        List<UserInfoResponse> response = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success("조회 성공", response));
    }

    @GetMapping("/pageable")
    @Operation(summary = "사용자 목록 조회 (페이징)", description = "사용자 목록을 페이징하여 조회 (ADMIN 권한 필요)")
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<UserInfoResponse>>> getAllUsersPageable(Pageable pageable) {
        Page<UserInfoResponse> response = userService.getAllUsersPageable(pageable);
        return ResponseEntity.ok(ApiResponse.success("조회 성공", response));
    }

    @GetMapping("/search")
    @Operation(summary = "사용자 검색", description = "키워드로 사용자 검색 (ADMIN 권한 필요)")
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserInfoResponse>>> searchUsers(
            @RequestParam(required = false) String keyword) {
        List<UserInfoResponse> response = userService.searchUsers(keyword);
        return ResponseEntity.ok(ApiResponse.success("검색 성공", response));
    }

    @GetMapping("/search/pageable")
    @Operation(summary = "사용자 검색 (페이징)", description = "키워드로 사용자를 검색하여 페이징으로 조회 (ADMIN 권한 필요)")
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<UserInfoResponse>>> searchUsersPageable(
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        Page<UserInfoResponse> response = userService.searchUsersPageable(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success("검색 성공", response));
    }

    @PutMapping("/{userId}")
    @Operation(summary = "사용자 정보 수정", description = "사용자 정보 수정 (ADMIN 권한 필요)")
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserInfoResponse>> updateUser(
            @PathVariable @NotNull(message = "userId는 필수입니다") Long userId,
            @Valid @RequestBody UserUpdateRequest updateRequest) {
        UserInfoResponse response = userService.updateUser(userId, updateRequest);
        return ResponseEntity.ok(ApiResponse.success("수정 성공", response));
    }

    @PostMapping("/{userId}/activate")
    @Operation(summary = "사용자 활성화", description = "사용자를 활성화합니다 (ADMIN 권한 필요)")
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> activateUser(
            @PathVariable @NotNull(message = "userId는 필수입니다") Long userId) {
        userService.activateUser(userId);
        return ResponseEntity.ok(ApiResponse.success("사용자가 활성화되었습니다"));
    }

    @PostMapping("/{userId}/deactivate")
    @Operation(summary = "사용자 비활성화", description = "사용자를 비활성화합니다 (ADMIN 권한 필요)")
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(
            @PathVariable @NotNull(message = "userId는 필수입니다") Long userId) {
        userService.deactivateUser(userId);
        return ResponseEntity.ok(ApiResponse.success("사용자가 비활성화되었습니다"));
    }

    @PostMapping("/{userId}/promote")
    @Operation(summary = "관리자로 승격", description = "사용자를 관리자로 승격합니다 (ADMIN 권한 필요)")
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> promoteToAdmin(
            @PathVariable @NotNull(message = "userId는 필수입니다") Long userId) {
        userService.promoteToAdmin(userId);
        return ResponseEntity.ok(ApiResponse.success("사용자가 관리자로 승격되었습니다"));
    }

    @PostMapping("/{userId}/demote")
    @Operation(summary = "일반 사용자로 강등", description = "관리자를 일반 사용자로 강등합니다 (ADMIN 권한 필요)")
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> demoteToUser(
            @PathVariable @NotNull(message = "userId는 필수입니다") Long userId) {
        userService.demoteToUser(userId);
        return ResponseEntity.ok(ApiResponse.success("사용자가 일반 사용자로 강등되었습니다"));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "사용자 삭제", description = "사용자를 영구적으로 삭제합니다 (ADMIN 권한 필요)")
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable @NotNull(message = "userId는 필수입니다") Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success("사용자가 삭제되었습니다"));
    }

    @GetMapping("/stats/count-by-role/{role}")
    @Operation(summary = "역할별 사용자 수 조회", description = "특정 역할의 사용자 수를 조회합니다 (ADMIN 권한 필요)")
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Long>> getUserCountByRole(
            @PathVariable String role) {
        Long count = userService.getUserCountByRole(com.festapp.dashboard.user.entity.User.Role.valueOf(role.toUpperCase()));
        return ResponseEntity.ok(ApiResponse.success("조회 성공", count));
    }

    @GetMapping("/stats/count-active")
    @Operation(summary = "활성 사용자 수 조회", description = "활성화된 사용자 수를 조회합니다 (ADMIN 권한 필요)")
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Long>> getActiveUserCount() {
        Long count = userService.getActiveUserCount();
        return ResponseEntity.ok(ApiResponse.success("조회 성공", count));
    }

    @GetMapping("/stats/count-inactive")
    @Operation(summary = "비활성 사용자 수 조회", description = "비활성화된 사용자 수를 조회합니다 (ADMIN 권한 필요)")
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Long>> getInactiveUserCount() {
        Long count = userService.getInactiveUserCount();
        return ResponseEntity.ok(ApiResponse.success("조회 성공", count));
    }
}



