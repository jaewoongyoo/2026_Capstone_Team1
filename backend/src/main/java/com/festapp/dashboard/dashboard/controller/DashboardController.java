package com.festapp.dashboard.dashboard.controller;

import com.festapp.dashboard.common.dto.ApiResponse;
import com.festapp.dashboard.common.security.SecurityContextHelper;
import com.festapp.dashboard.dashboard.dto.DashboardRequest;
import com.festapp.dashboard.dashboard.dto.DashboardResponse;
import com.festapp.dashboard.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboards")
@RequiredArgsConstructor
@Tag(name = "Dashboards", description = "프론트에서 대시보드 목록/상세/수정 화면을 구성할 때 사용하는 API입니다. 로그인 직후에는 보통 이 태그의 목록 조회부터 호출합니다.")
@SecurityRequirement(name = "bearer-jwt")
public class DashboardController {

    private final DashboardService dashboardService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "대시보드 생성", description = "프론트 사용 시점: 대시보드 추가 버튼 저장 시 호출합니다.")
    public ResponseEntity<ApiResponse<DashboardResponse>> createDashboard(@Valid @RequestBody DashboardRequest request) {
        DashboardResponse response = dashboardService.createDashboard(SecurityContextHelper.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("대시보드 생성 성공", response, HttpStatus.CREATED.value()));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "내 대시보드 목록 조회", description = "프론트 사용 시점: 로그인 직후 홈 화면, 사이드바, 대시보드 선택 드롭다운을 그릴 때 호출합니다.")
    public ResponseEntity<ApiResponse<List<DashboardResponse>>> getMyDashboards() {
        List<DashboardResponse> response = dashboardService.getMyDashboards(SecurityContextHelper.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("조회 성공", response));
    }

    @GetMapping("/{dashboardId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "대시보드 단건 조회", description = "프론트 사용 시점: 특정 대시보드 상세 화면 진입 시 기본 메타정보를 읽을 때 호출합니다.")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(@PathVariable Long dashboardId) {
        DashboardResponse response = dashboardService.getDashboard(SecurityContextHelper.getCurrentUserId(), dashboardId);
        return ResponseEntity.ok(ApiResponse.success("조회 성공", response));
    }

    @PutMapping("/{dashboardId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "대시보드 수정", description = "프론트 사용 시점: 대시보드 이름/설명 편집 저장 시 호출합니다.")
    public ResponseEntity<ApiResponse<DashboardResponse>> updateDashboard(
            @PathVariable Long dashboardId,
            @Valid @RequestBody DashboardRequest request) {
        DashboardResponse response = dashboardService.updateDashboard(SecurityContextHelper.getCurrentUserId(), dashboardId, request);
        return ResponseEntity.ok(ApiResponse.success("수정 성공", response));
    }

    @DeleteMapping("/{dashboardId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "대시보드 삭제", description = "프론트 사용 시점: 대시보드 삭제 확인 후 실제 제거 시 호출합니다.")
    public ResponseEntity<ApiResponse<Void>> deleteDashboard(@PathVariable Long dashboardId) {
        dashboardService.deleteDashboard(SecurityContextHelper.getCurrentUserId(), dashboardId);
        return ResponseEntity.ok(ApiResponse.success("삭제 성공"));
    }

    @PostMapping("/{dashboardId}/share/enable")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "대시보드 공유 활성화", description = "대시보드를 외부에 공개할 수 있는 UUID 토큰 링크를 활급하고 공개 설정합니다.")
    public ResponseEntity<ApiResponse<DashboardResponse>> enableShare(@PathVariable Long dashboardId) {
        DashboardResponse response = dashboardService.enableShare(SecurityContextHelper.getCurrentUserId(), dashboardId);
        return ResponseEntity.ok(ApiResponse.success("공유 활성화 성공", response));
    }

    @PostMapping("/{dashboardId}/share/disable")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "대시보드 공유 비활성화", description = "대시보드 공유 링크를 해제하고 비공개 설정합니다.")
    public ResponseEntity<ApiResponse<DashboardResponse>> disableShare(@PathVariable Long dashboardId) {
        DashboardResponse response = dashboardService.disableShare(SecurityContextHelper.getCurrentUserId(), dashboardId);
        return ResponseEntity.ok(ApiResponse.success("공유 비활성화 성공", response));
    }
}
