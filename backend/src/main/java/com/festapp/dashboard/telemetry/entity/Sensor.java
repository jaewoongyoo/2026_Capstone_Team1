package com.festapp.dashboard.telemetry.entity;

import com.festapp.dashboard.equipment.entity.Equipment;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "sensor",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_sensor_equipment_name", columnNames = {"equipment_id", "sensor_name"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "sensorId")
@ToString(exclude = "equipment")
public class Sensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sensor_id")
    private Long sensorId;

    @Column(name = "sensor_name", nullable = false, length = 255)
    private String sensorName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;
}
