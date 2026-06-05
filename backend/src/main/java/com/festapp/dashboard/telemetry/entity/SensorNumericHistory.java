package com.festapp.dashboard.telemetry.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_numeric_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "sensorNumericHistoryId")
@ToString(exclude = "sensor")
public class SensorNumericHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sensor_numeric_history_id")
    private Long sensorNumericHistoryId;

    @Column(name = "numeric_value", nullable = false)
    private Double value;

    @Column(name = "data_type", nullable = false, length = 50)
    private String dataType;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "unit", length = 255)
    private String unit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sensor_id", nullable = false)
    private Sensor sensor;
}
