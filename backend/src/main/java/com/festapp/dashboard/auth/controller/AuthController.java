// 인증 제어자 - 로그인, 회원가입, 토큰 갱신 등 사용자 인증 관련 API 처리
package com.festapp.dashboard.auth.controller;

import com.festapp.dashboard.auth.dto.*;
import com.festapp.dashboard.auth.service.AuthService;
import com.festapp.dashboard.common.dto.ApiResponse;
import com.festapp.dashboard.common.security.SecurityContextHelper;
import com.festapp.dashboard.user.dto.UserInfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 제어자
 *
 * 회원가입, 로그인, 로그아웃, 토큰 갱신 등 인증 관련 API를 제공합니다.
 *
 * @author DashBoar Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "회원가입, 로그인, 로그아웃 등 인증 관련 API")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다\n\n" +
            "**테스트 예시:**\n" +
            "- username: testuser\n" +
            "- email: test@example.com\n" +
            "- password: Test123!@# (영문+숫자+특수문자)\n" +
            "- fullName: 테스트 사용자")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponse<UserInfoResponse>> signUp(@Valid @RequestBody SignUpRequest request) {
        UserInfoResponse response = authService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입 성공", response));
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "사용자 인증 및 JWT 토큰 발급\n\n" +
            "**관리자 테스트 계정:**\n" +
            "- username: admin\n" +
            "- password: Admin123!")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("로그인 성공", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 재발급", description = "Refresh Token으로 새로운 Access Token 발급")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "유효하지 않은 토큰")
    })
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshAccessToken(request);
        return ResponseEntity.ok(ApiResponse.success("토큰 재발급 성공", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "현재 사용자 로그아웃")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<ApiResponse<Void>> logout() {
        authService.logout();
        return ResponseEntity.ok(ApiResponse.success("로그아웃 성공"));
    }

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "현재 인증된 사용자의 정보 조회")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<ApiResponse<UserInfoResponse>> getMe() {
        Long userId = SecurityContextHelper.getCurrentUserId();
        UserInfoResponse response = authService.getUserInfo(userId);
        return ResponseEntity.ok(ApiResponse.success("사용자 정보 조회 성공", response));
    }

    @PostMapping("/change-password")
    @Operation(summary = "비밀번호 변경", description = "현재 사용자의 비밀번호를 변경합니다")
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = SecurityContextHelper.getCurrentUserId();
        authService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success("비밀번호 변경 성공"));
    }

    @GetMapping("/token-stats")
    @Operation(summary = "토큰 통계 조회", description = "현재 사용자의 활성 토큰, 만료된 토큰 등의 통계 조회")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<ApiResponse<TokenStatsResponse>> getTokenStats() {
        Long userId = SecurityContextHelper.getCurrentUserId();
        TokenStatsResponse response = authService.getTokenStats(userId);
        return ResponseEntity.ok(ApiResponse.success("토큰 통계 조회 성공", response));
    }

    @PostMapping("/token-stats/cleanup")
    @Operation(summary = "만료된 토큰 정리", description = "현재 사용자의 만료된 토큰을 정리합니다")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<Long>> cleanupExpiredTokens() {
        Long userId = SecurityContextHelper.getCurrentUserId();
        long cleanedCount = authService.cleanupExpiredTokens(userId);
        return ResponseEntity.ok(ApiResponse.success("만료된 토큰 정리 완료", cleanedCount));
    }

    @PostMapping("/revoke-all-tokens")
    @Operation(summary = "모든 토큰 폐지", description = "현재 사용자의 모든 토큰을 폐지합니다 (모든 디바이스에서 로그아웃)")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<Void>> revokeAllTokens() {
        Long userId = SecurityContextHelper.getCurrentUserId();
        authService.revokeAllTokens(userId);
        return ResponseEntity.ok(ApiResponse.success("모든 토큰이 폐지되었습니다"));
    }
}


