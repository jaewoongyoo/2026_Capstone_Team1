package com.festapp.dashboard.dashboard.controller;

import com.festapp.dashboard.common.dto.ApiResponse;
import com.festapp.dashboard.dashboard.dto.PublicDashboardResponse;
import com.festapp.dashboard.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/dashboards")
@RequiredArgsConstructor
@Tag(name = "Public Dashboards", description = "비로그인 상태에서 토큰을 통해 접근할 수 있는 대시보드 API입니다.")
public class PublicDashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @Operation(summary = "공유 대시보드 단건 조회", description = "비로그인 외부 사용자가 유효한 토큰(UUID)을 이용하여 대시보드 상세 구조를 조회합니다.")
    public ResponseEntity<ApiResponse<PublicDashboardResponse>> getPublicDashboard(@RequestParam String token) {
        PublicDashboardResponse response = dashboardService.getPublicDashboard(token);
        return ResponseEntity.ok(ApiResponse.success("공유 대시보드 조회 성공", response));
    }
}
