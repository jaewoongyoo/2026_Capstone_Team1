package com.festapp.dashboard.telemetry.controller;

import com.festapp.dashboard.common.dto.ApiResponse;
import com.festapp.dashboard.telemetry.dto.SensorDataPayload;
import com.festapp.dashboard.telemetry.service.RealTimeDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/telemetry")
@RequiredArgsConstructor
@Tag(name = "Telemetry", description = "Kafka/RabbitMQ 연동 전 프론트 실시간 화면을 검증하기 위한 테스트용 telemetry API입니다.")
@SecurityRequirement(name = "bearer-jwt")
public class TelemetryTestController {

    private final RealTimeDataService realTimeDataService;

    @PostMapping("/test")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "실시간 센서 데이터 테스트 입력", description = "프론트/테스트 사용 시점: Kafka 없이도 DB 저장, Redis 최신값 갱신, WebSocket 브로드캐스트 흐름을 검증할 때 호출합니다.")
    public ResponseEntity<ApiResponse<SensorDataPayload>> publishTestTelemetry(
            @Valid @RequestBody SensorDataPayload payload) {
        if (!realTimeDataService.processSensorData(payload)) {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(ApiResponse.success("Telemetry 대상 장비가 없어 저장과 브로드캐스트를 건너뛰었습니다", payload, HttpStatus.ACCEPTED.value()));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Telemetry 테스트 데이터 처리 성공", payload, HttpStatus.CREATED.value()));
    }
}
