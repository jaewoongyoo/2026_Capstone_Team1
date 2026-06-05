package com.festapp.dashboard.dashboard.widget.entity;

import com.festapp.dashboard.dashboard.entity.Dashboard;
import com.festapp.dashboard.equipment.entity.Equipment;
import com.festapp.dashboard.telemetry.entity.Sensor;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "widgets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "widgetId")
@ToString(exclude = {"dashboard", "equipment", "sensor"})
public class DashboardWidget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "widget_id")
    private Long widgetId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dashboard_id", nullable = false)
    private Dashboard dashboard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id")
    private Equipment equipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_id")
    private Sensor sensor;

    @Column(name = "widget_type", nullable = false, length = 50)
    private String widgetType;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "chart_type", length = 50)
    private String chartType;

    @Column(name = "data_type", length = 50)
    private String dataType;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "pos_x", nullable = false)
    @Builder.Default
    private Integer posX = 0;

    @Column(name = "pos_y", nullable = false)
    @Builder.Default
    private Integer posY = 0;

    @Column(name = "width", nullable = false)
    @Builder.Default
    private Integer width = 1;

    @Column(name = "height", nullable = false)
    @Builder.Default
    private Integer height = 1;

    @Column(name = "config_json", columnDefinition = "TEXT")
    private String configJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return widgetId;
    }
}
