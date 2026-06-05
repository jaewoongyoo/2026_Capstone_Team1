package com.festapp.dashboard.telemetry.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
@Schema(description = "숫자형 센서 이력 입력 요청")
public class SensorNumericHistoryCreateRequest {

    @NotNull
    @Schema(description = "센서 측정값", example = "24.1")
    private Double value;

    @NotBlank
    @Pattern(regexp = "FLOAT|DOUBLE|INTEGER|INT", message = "dataType must be one of FLOAT, DOUBLE, INTEGER, INT")
    @Schema(description = "데이터 타입", example = "FLOAT")
    private String dataType;

    @Schema(description = "측정 단위", example = "C")
    private String unit;

    @Schema(description = "측정 시각. 없으면 현재 시각으로 저장됩니다", example = "2026-04-17T21:30:00")
    private LocalDateTime timestamp;
}
