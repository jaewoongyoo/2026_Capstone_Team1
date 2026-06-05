package com.festapp.dashboard.telemetry.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "문자형 센서 이력 입력 요청")
public class SensorStringHistoryCreateRequest {

    @NotBlank
    @Schema(description = "센서 상태값", example = "RUNNING")
    private String status;

    @Schema(description = "측정 시각. 없으면 현재 시각으로 저장됩니다", example = "2026-04-17T21:30:00")
    private LocalDateTime timestamp;
}
