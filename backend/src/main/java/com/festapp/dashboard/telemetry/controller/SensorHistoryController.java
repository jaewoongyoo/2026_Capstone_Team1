package com.festapp.dashboard.telemetry.controller;

import com.festapp.dashboard.common.dto.ApiResponse;
import com.festapp.dashboard.common.security.SecurityContextHelper;
import com.festapp.dashboard.telemetry.dto.SensorNumericHistoryCreateRequest;
import com.festapp.dashboard.telemetry.dto.SensorNumericHistoryResponse;
import com.festapp.dashboard.telemetry.dto.SensorStringHistoryCreateRequest;
import com.festapp.dashboard.telemetry.dto.SensorStringHistoryResponse;
import com.festapp.dashboard.telemetry.service.SensorHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/sensors/{sensorId}/history")
@RequiredArgsConstructor
@Tag(name = "Sensor History", description = "센서 시계열 이력을 읽고 저장하는 API입니다. 프론트 차트/상태 패널은 조회 API를, 테스트 도구나 수집 어댑터는 입력 API를 사용할 수 있습니다.")
@SecurityRequirement(name = "bearer-jwt")
public class SensorHistoryController {

    private final SensorHistoryService sensorHistoryService;

    @PostMapping("/numeric")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "숫자형 센서 이력 입력", description = "프론트/도구 사용 시점: 시뮬레이터, 테스트 페이지, 수집 어댑터가 숫자형 측정값을 저장할 때 사용합니다.")
    public ResponseEntity<ApiResponse<SensorNumericHistoryResponse>> createNumericHistory(
            @PathVariable Long sensorId,
            @Valid @RequestBody SensorNumericHistoryCreateRequest request) {
        SensorNumericHistoryResponse response = sensorHistoryService.createNumericHistory(
                SecurityContextHelper.getCurrentUserId(),
                sensorId,
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("숫자형 센서 이력 저장 성공", response, HttpStatus.CREATED.value()));
    }

    @GetMapping("/numeric")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "숫자형 센서 이력 조회", description = "프론트 사용 시점: 차트 위젯, 게이지 위젯, 수치 카드에서 최근 숫자형 시계열 데이터를 읽을 때 호출합니다.")
    public ResponseEntity<ApiResponse<List<SensorNumericHistoryResponse>>> getNumericHistory(
            @PathVariable Long sensorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        List<SensorNumericHistoryResponse> response = sensorHistoryService.getNumericHistory(
                SecurityContextHelper.getCurrentUserId(),
                sensorId,
                from,
                to
        );
        return ResponseEntity.ok(ApiResponse.success("조회 성공", response));
    }

    @PostMapping("/string")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "문자형 센서 이력 입력", description = "프론트/도구 사용 시점: 시뮬레이터, 테스트 페이지, 수집 어댑터가 상태값 문자열 이력을 저장할 때 사용합니다.")
    public ResponseEntity<ApiResponse<SensorStringHistoryResponse>> createStringHistory(
            @PathVariable Long sensorId,
            @Valid @RequestBody SensorStringHistoryCreateRequest request) {
        SensorStringHistoryResponse response = sensorHistoryService.createStringHistory(
                SecurityContextHelper.getCurrentUserId(),
                sensorId,
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("문자형 센서 이력 저장 성공", response, HttpStatus.CREATED.value()));
    }

    @GetMapping("/string")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "문자형 센서 이력 조회", description = "프론트 사용 시점: 상태 패널, 이벤트 타임라인, 최근 상태 목록을 그릴 때 호출합니다.")
    public ResponseEntity<ApiResponse<List<SensorStringHistoryResponse>>> getStringHistory(
            @PathVariable Long sensorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        List<SensorStringHistoryResponse> response = sensorHistoryService.getStringHistory(
                SecurityContextHelper.getCurrentUserId(),
                sensorId,
                from,
                to
        );
        return ResponseEntity.ok(ApiResponse.success("조회 성공", response));
    }
}
