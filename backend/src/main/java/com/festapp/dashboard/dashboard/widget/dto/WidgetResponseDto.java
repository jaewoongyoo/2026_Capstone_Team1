package com.festapp.dashboard.dashboard.widget.dto;

import com.festapp.dashboard.dashboard.widget.entity.DashboardWidget;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "위젯 응답 DTO")
public class WidgetResponseDto {

    @Schema(description = "위젯 ID", example = "1")
    private Long id;

    @Schema(description = "소유자 사용자 ID", example = "100")
    private Long userId;

    @Schema(description = "대시보드 ID", example = "1")
    private Long dashboardId;

    @Schema(description = "대시보드 이름", example = "Main Dashboard")
    private String dashboardName;

    @Schema(description = "레거시 장비 식별자(장비명 기반)", example = "CVD-CHAMBER-01")
    private String equipmentId;

    @Schema(description = "장비 엔티티 ID", example = "10")
    private Long equipmentEntityId;

    @Schema(description = "장비 이름", example = "CVD Chamber 01")
    private String equipmentName;

    @Schema(description = "위젯 타입", example = "GAUGE")
    private String widgetType;

    @Schema(description = "위젯 제목", example = "Temperature Gauge")
    private String title;

    @Schema(description = "레거시 센서 식별자(센서명 기반)", example = "Temp_Sensor_001")
    private String sensorId;

    @Schema(description = "센서 엔티티 ID", example = "100")
    private Long sensorEntityId;

    @Schema(description = "센서 이름", example = "Temp Sensor")
    private String sensorName;

    @Schema(description = "차트 타입", example = "line")
    private String chartType;

    @Schema(description = "데이터 타입", example = "FLOAT")
    private String dataType;

    @Schema(description = "단위", example = "C")
    private String unit;

    @Schema(description = "X 위치", example = "0")
    private Integer posX;

    @Schema(description = "Y 위치", example = "0")
    private Integer posY;

    @Schema(description = "너비", example = "4")
    private Integer width;

    @Schema(description = "높이", example = "3")
    private Integer height;

    @Schema(description = "추가 설정 정보", example = "{\"refreshInterval\":5000}")
    private String configJson;

    @Schema(description = "생성 시각", example = "2026-04-03T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정 시각", example = "2026-04-03T11:45:30")
    private LocalDateTime updatedAt;

    public static WidgetResponseDto fromEntity(DashboardWidget widget) {
        return WidgetResponseDto.builder()
                .id(widget.getWidgetId())
                .userId(widget.getDashboard().getUser().getUserId())
                .dashboardId(widget.getDashboard().getDashboardId())
                .dashboardName(widget.getDashboard().getDashboardName())
                .equipmentId(widget.getEquipment() != null ? widget.getEquipment().getEquipmentName() : null)
                .equipmentEntityId(widget.getEquipment() != null ? widget.getEquipment().getEquipmentId() : null)
                .equipmentName(widget.getEquipment() != null ? widget.getEquipment().getEquipmentName() : null)
                .widgetType(widget.getWidgetType())
                .title(widget.getTitle())
                .sensorId(widget.getSensor() != null ? widget.getSensor().getSensorName() : null)
                .sensorEntityId(widget.getSensor() != null ? widget.getSensor().getSensorId() : null)
                .sensorName(widget.getSensor() != null ? widget.getSensor().getSensorName() : null)
                .chartType(widget.getChartType())
                .dataType(widget.getDataType())
                .unit(widget.getUnit())
                .posX(widget.getPosX())
                .posY(widget.getPosY())
                .width(widget.getWidth())
                .height(widget.getHeight())
                .configJson(widget.getConfigJson())
                .createdAt(widget.getCreatedAt())
                .updatedAt(widget.getUpdatedAt())
                .build();
    }
}
