package com.festapp.dashboard.telemetry.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "센서 생성/수정 요청")
public class SensorRequest {

    @NotNull(message = "Equipment ID는 필수입니다")
    @Schema(description = "소속 장비 ID", example = "10")
    private Long equipmentId;

    @NotBlank(message = "Sensor name은 필수입니다")
    @Size(max = 255, message = "Sensor name은 255자 이하여야 합니다")
    @Schema(description = "센서 이름", example = "Temp_Sensor_001")
    private String sensorName;
}
