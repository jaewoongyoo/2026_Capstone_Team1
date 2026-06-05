package com.festapp.dashboard.dashboard.widget.controller;

import com.festapp.dashboard.common.dto.ApiResponse;
import com.festapp.dashboard.common.security.SecurityContextHelper;
import com.festapp.dashboard.dashboard.widget.dto.WidgetRequestDto;
import com.festapp.dashboard.dashboard.widget.dto.WidgetResponseDto;
import com.festapp.dashboard.dashboard.widget.service.DashboardWidgetService;
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
@RequiredArgsConstructor
@Tag(name = "Dashboard Widget Resources", description = "새 ERD 기준 위젯 API입니다. 프론트에서는 대시보드 상세 화면을 그릴 때 가장 자주 사용하며, 가능하면 equipmentEntityId / sensorEntityId 기준으로 연동하는 것을 권장합니다.")
@SecurityRequirement(name = "bearer-jwt")
public class DashboardWidgetResourceController {

    private final DashboardWidgetService widgetService;

    @GetMapping("/api/dashboards/{dashboardId}/widgets")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "대시보드별 위젯 목록 조회", description = "프론트 사용 시점: 대시보드 상세 페이지 최초 진입 시 위젯 레이아웃과 데이터 연결 정보를 불러올 때 호출합니다.")
    public ResponseEntity<ApiResponse<List<WidgetResponseDto>>> getDashboardWidgets(@PathVariable Long dashboardId) {
        List<WidgetResponseDto> response = widgetService.getDashboardWidgets(SecurityContextHelper.getCurrentUserId(), dashboardId);
        return ResponseEntity.ok(ApiResponse.success("조회 성공", response));
    }

    @PostMapping("/api/dashboards/{dashboardId}/widgets")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "대시보드에 위젯 생성", description = "프론트 사용 시점: 위젯 추가 모달 저장 시 호출합니다. 새 구현은 레거시 문자열 ID 대신 equipmentEntityId, sensorEntityId 사용을 권장합니다.")
    public ResponseEntity<ApiResponse<WidgetResponseDto>> createDashboardWidget(
            @PathVariable Long dashboardId,
            @Valid @RequestBody WidgetRequestDto request) {
        WidgetRequestDto normalizedRequest = request.toBuilder()
                .dashboardId(dashboardId)
                .build();

        WidgetResponseDto response = widgetService.createWidget(SecurityContextHelper.getCurrentUserId(), normalizedRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("위젯 생성 성공", response, HttpStatus.CREATED.value()));
    }

    @GetMapping("/api/equipment/{equipmentId}/widgets")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "장비 엔티티 ID 기준 위젯 목록 조회", description = "프론트 사용 시점: 특정 장비와 연결된 위젯만 별도로 필터링해 보여줄 때 사용합니다.")
    public ResponseEntity<ApiResponse<List<WidgetResponseDto>>> getEquipmentWidgets(@PathVariable Long equipmentId) {
        List<WidgetResponseDto> response = widgetService.getMyWidgetsByEquipmentEntityId(SecurityContextHelper.getCurrentUserId(), equipmentId);
        return ResponseEntity.ok(ApiResponse.success("조회 성공", response));
    }
}
