// 대시보드 위젯 제어자 - 위젯 생성, 조회, 수정, 삭제 API 처리
package com.festapp.dashboard.dashboard.widget.controller;

import com.festapp.dashboard.dashboard.widget.dto.WidgetLayoutUpdateDto;
import com.festapp.dashboard.dashboard.widget.dto.WidgetRequestDto;
import com.festapp.dashboard.dashboard.widget.dto.WidgetResponseDto;
import com.festapp.dashboard.dashboard.widget.service.DashboardWidgetService;
import com.festapp.dashboard.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/dashboard/widgets")
@RequiredArgsConstructor
@Tag(name = "Dashboard Widgets", description = "대시보드 위젯 관리 API - 사용자는 자신의 위젯만 조회/수정/삭제할 수 있습니다")
@SecurityRequirement(name = "bearer-jwt")
public class DashboardWidgetController {

    private final DashboardWidgetService widgetService;


    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "위젯 생성", description = "새로운 대시보드 위젯을 생성합니다. 위젯 레이아웃 위치는 posX, posY, width, height로 지정합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "위젯 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 검증 실패 (필수 필드 누락, 형식 오류 등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 또는 토큰 만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<WidgetResponseDto>> createWidget(
            @Valid @RequestBody WidgetRequestDto request) {

        Long userId = getAuthenticatedUserId();
        WidgetResponseDto response = widgetService.createWidget(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<WidgetResponseDto>builder()
                        .success(true)
                        .message("Widget created successfully")
                        .data(response)
                        .statusCode(HttpStatus.CREATED.value())
                        .build());
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "내 위젯 전체 조회", description = "로그인한 사용자의 모든 대시보드 위젯을 조회합니다. 정렬 순서: ID 오름차순")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공 (빈 배열일 수 있음)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 또는 토큰 만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<List<WidgetResponseDto>>> getMyWidgets() {

        Long userId = getAuthenticatedUserId();
        List<WidgetResponseDto> responses = widgetService.getMyWidgets(userId);

        return ResponseEntity.ok()
                .body(ApiResponse.<List<WidgetResponseDto>>builder()
                        .success(true)
                        .message("Widgets retrieved successfully")
                        .data(responses)
                        .statusCode(HttpStatus.OK.value())
                        .build());
    }

    @GetMapping("/equipment/{equipmentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "장비별 위젯 조회", description = "특정 장비의 대시보드 위젯을 조회합니다. 로그인한 사용자의 위젯만 반환됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공 (빈 배열일 수 있음)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "equipmentId가 유효하지 않음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 또는 토큰 만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<List<WidgetResponseDto>>> getMyWidgetsByEquipment(
            @PathVariable @NotNull(message = "equipmentId는 필수입니다") @Parameter(description = "장비 ID") String equipmentId) {

        Long userId = getAuthenticatedUserId();
        List<WidgetResponseDto> responses = widgetService.getMyWidgetsByEquipment(userId, equipmentId);

        return ResponseEntity.ok()
                .body(ApiResponse.<List<WidgetResponseDto>>builder()
                        .success(true)
                        .message("Widgets retrieved successfully")
                        .data(responses)
                        .statusCode(HttpStatus.OK.value())
                        .build());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "위젯 단건 조회", description = "특정 위젯의 상세 정보를 조회합니다. 자신이 소유한 위젯만 조회할 수 있습니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 또는 토큰 만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "위젯을 찾을 수 없음 (존재하지 않거나 다른 사용자의 위젯)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<WidgetResponseDto>> getWidget(
            @PathVariable @NotNull(message = "위젯 ID는 필수입니다") @Parameter(description = "위젯 ID") Long id) {

        Long userId = getAuthenticatedUserId();
        WidgetResponseDto response = widgetService.getWidget(userId, id);

        return ResponseEntity.ok()
                .body(ApiResponse.<WidgetResponseDto>builder()
                        .success(true)
                        .message("Widget retrieved successfully")
                        .data(response)
                        .statusCode(HttpStatus.OK.value())
                        .build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "위젯 수정", description = "기존 대시보드 위젯을 수정합니다. 자신이 소유한 위젯만 수정할 수 있습니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 검증 실패 (필수 필드 누락, 형식 오류 등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 또는 토큰 만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "위젯을 찾을 수 없음 (존재하지 않거나 다른 사용자의 위젯)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<WidgetResponseDto>> updateWidget(
            @PathVariable @NotNull(message = "위젯 ID는 필수입니다") @Parameter(description = "위젯 ID") Long id,
            @Valid @RequestBody WidgetRequestDto request) {

        Long userId = getAuthenticatedUserId();
        WidgetResponseDto response = widgetService.updateWidget(userId, id, request);

        return ResponseEntity.ok()
                .body(ApiResponse.<WidgetResponseDto>builder()
                        .success(true)
                        .message("Widget updated successfully")
                        .data(response)
                        .statusCode(HttpStatus.OK.value())
                        .build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "위젯 삭제", description = "대시보드 위젯을 삭제합니다. 자신이 소유한 위젯만 삭제할 수 있습니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공 (응답 본문 없음)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 또는 토큰 만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "위젯을 찾을 수 없음 (존재하지 않거나 다른 사용자의 위젯)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Void> deleteWidget(
            @PathVariable @NotNull(message = "위젯 ID는 필수입니다") @Parameter(description = "위젯 ID") Long id) {

        Long userId = getAuthenticatedUserId();
        widgetService.deleteWidget(userId, id);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/layout")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "위젯 레이아웃 일괄 저장", description = "여러 위젯의 위치 및 크기 정보를 일괄 저장합니다. 존재하지 않는 위젯 요청이 있으면 전체 요청이 실패합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "저장 성공 (모든 위젯의 업데이트된 정보 반환)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 검증 실패 (필수 필드 누락, 형식 오류 등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 또는 토큰 만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "위젯을 찾을 수 없음 (존재하지 않거나 다른 사용자의 위젯)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<List<WidgetResponseDto>>> updateLayouts(
            @Valid @RequestBody WidgetLayoutUpdateDto request) {

        Long userId = getAuthenticatedUserId();
        List<WidgetResponseDto> responses = widgetService.updateLayouts(userId, request);

        return ResponseEntity.ok()
                .body(ApiResponse.<List<WidgetResponseDto>>builder()
                        .success(true)
                        .message("Layouts updated successfully")
                        .data(responses)
                        .statusCode(HttpStatus.OK.value())
                        .build());
    }

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("User is not authenticated");
            throw new IllegalStateException("User is not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof com.festapp.dashboard.common.security.CustomUserDetails) {
            Long userId = ((com.festapp.dashboard.common.security.CustomUserDetails) principal).getUserId();
            log.debug("Extracted userId from authentication: {}", userId);
            return userId;
        }

        log.error("Cannot extract user ID from authentication. Principal type: {}", principal.getClass().getName());
        throw new IllegalStateException("Cannot extract user ID from authentication");
    }
}

