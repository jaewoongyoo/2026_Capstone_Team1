package com.festapp.dashboard.equipment.controller;

import com.festapp.dashboard.common.dto.ApiResponse;
import com.festapp.dashboard.common.security.SecurityContextHelper;
import com.festapp.dashboard.equipment.dto.DiscoveryApplyRequest;
import com.festapp.dashboard.equipment.dto.DiscoveryApplyResponse;
import com.festapp.dashboard.equipment.dto.EquipmentCurrentResponse;
import com.festapp.dashboard.equipment.dto.EquipmentRequest;
import com.festapp.dashboard.equipment.dto.EquipmentResponse;
import com.festapp.dashboard.equipment.service.EquipmentService;
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
@RequestMapping("/api/equipment")
@RequiredArgsConstructor
@Tag(name = "Equipment", description = "대시보드 아래에 속한 장비를 관리하는 API입니다. 프론트에서는 대시보드 편집 화면이나 장비 목록 패널에서 주로 사용합니다.")
@SecurityRequirement(name = "bearer-jwt")
public class EquipmentController {

    private final EquipmentService equipmentService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "장비 생성", description = "프론트 사용 시점: 대시보드 편집 화면에서 장비 추가 모달 저장 시 호출합니다.")
    public ResponseEntity<ApiResponse<EquipmentResponse>> createEquipment(@Valid @RequestBody EquipmentRequest request) {
        EquipmentResponse response = equipmentService.createEquipment(SecurityContextHelper.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("장비 생성 성공", response, HttpStatus.CREATED.value()));
    }

    @PostMapping("/discovery/apply")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "자동 검색 결과 일괄 등록", description = "프론트 사용 시점: 자동 검색 모달의 적용 및 등록 버튼 클릭 시 발견된 장비와 태그를 한 번에 장비/센서로 생성하거나 기존 항목을 재사용합니다.")
    public ResponseEntity<ApiResponse<DiscoveryApplyResponse>> applyDiscovery(
            @Valid @RequestBody DiscoveryApplyRequest request) {
        DiscoveryApplyResponse response = equipmentService.applyDiscovery(
                SecurityContextHelper.getCurrentUserId(),
                request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("자동 검색 결과 등록 성공", response, HttpStatus.CREATED.value()));
    }

    @GetMapping("/dashboard/{dashboardId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "대시보드별 장비 목록 조회", description = "프론트 사용 시점: 선택한 대시보드 아래 장비 목록을 렌더링할 때 호출합니다.")
    public ResponseEntity<ApiResponse<List<EquipmentResponse>>> getDashboardEquipment(@PathVariable Long dashboardId) {
        List<EquipmentResponse> response = equipmentService.getDashboardEquipment(SecurityContextHelper.getCurrentUserId(), dashboardId);
        return ResponseEntity.ok(ApiResponse.success("조회 성공", response));
    }

    @GetMapping("/dashboard/{dashboardId}/search")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "대시보드별 장비 검색", description = "프론트 사용 시점: 설비 검색창에서 장비명 또는 분류(field)로 필터링할 때 호출합니다. keyword가 비어 있으면 전체 장비 목록을 반환합니다.")
    public ResponseEntity<ApiResponse<List<EquipmentResponse>>> searchDashboardEquipment(
            @PathVariable Long dashboardId,
            @RequestParam(required = false, defaultValue = "") String keyword) {
        List<EquipmentResponse> response = equipmentService.searchDashboardEquipment(
                SecurityContextHelper.getCurrentUserId(),
                dashboardId,
                keyword);
        return ResponseEntity.ok(ApiResponse.success("검색 성공", response));
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "내 전체 장비 검색", description = "프론트 사용 시점: dashboardId 없이 전체 설비 검색창에서 장비명 또는 분류(field)로 검색할 때 호출합니다. keyword가 비어 있으면 내가 가진 전체 장비 목록을 반환합니다.")
    public ResponseEntity<ApiResponse<List<EquipmentResponse>>> searchMyEquipment(
            @RequestParam(required = false, defaultValue = "") String keyword) {
        List<EquipmentResponse> response = equipmentService.searchUserEquipment(
                SecurityContextHelper.getCurrentUserId(),
                keyword);
        return ResponseEntity.ok(ApiResponse.success("검색 성공", response));
    }

    @GetMapping("/current")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "내 전체 장비 최신 상태 조회", description = "프론트 사용 시점: 화면 최초 진입 시 전체 설비 카드/상태판에 표시할 최신 센서 스냅샷을 조회합니다.")
    public ResponseEntity<ApiResponse<List<EquipmentCurrentResponse>>> getMyEquipmentCurrent() {
        List<EquipmentCurrentResponse> response = equipmentService.getMyEquipmentCurrent(SecurityContextHelper.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("조회 성공", response));
    }

    @GetMapping("/{equipmentId}/current")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "장비 최신 상태 조회", description = "프론트 사용 시점: 장비 상세 화면 진입 시 Redis에 저장된 최신 센서 스냅샷을 조회합니다.")
    public ResponseEntity<ApiResponse<EquipmentCurrentResponse>> getEquipmentCurrent(@PathVariable Long equipmentId) {
        EquipmentCurrentResponse response = equipmentService.getEquipmentCurrent(SecurityContextHelper.getCurrentUserId(), equipmentId);
        return ResponseEntity.ok(ApiResponse.success("조회 성공", response));
    }

    @GetMapping("/{equipmentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "장비 단건 조회", description = "프론트 사용 시점: 장비 상세 패널 또는 수정 모달을 열 때 단건 정보를 읽어올 때 사용합니다.")
    public ResponseEntity<ApiResponse<EquipmentResponse>> getEquipment(@PathVariable Long equipmentId) {
        EquipmentResponse response = equipmentService.getEquipment(SecurityContextHelper.getCurrentUserId(), equipmentId);
        return ResponseEntity.ok(ApiResponse.success("조회 성공", response));
    }

    @PutMapping("/{equipmentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "장비 수정", description = "프론트 사용 시점: 장비명/분류 수정 저장 시 호출합니다.")
    public ResponseEntity<ApiResponse<EquipmentResponse>> updateEquipment(
            @PathVariable Long equipmentId,
            @Valid @RequestBody EquipmentRequest request) {
        EquipmentResponse response = equipmentService.updateEquipment(SecurityContextHelper.getCurrentUserId(), equipmentId, request);
        return ResponseEntity.ok(ApiResponse.success("수정 성공", response));
    }

    @DeleteMapping("/{equipmentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "대시보드에서 장비 제거",
            description = "프론트 사용 시점: 설비 관리 화면에서 장비를 정리할 때 호출합니다. 장비/센서/히스토리 마스터 데이터는 보존하고, 해당 장비에 연결된 위젯만 제거합니다."
    )
    public ResponseEntity<ApiResponse<Void>> deleteEquipment(@PathVariable Long equipmentId) {
        equipmentService.deleteEquipment(SecurityContextHelper.getCurrentUserId(), equipmentId);
        return ResponseEntity.ok(ApiResponse.success("대시보드에서 장비 제거 성공"));
    }
}
