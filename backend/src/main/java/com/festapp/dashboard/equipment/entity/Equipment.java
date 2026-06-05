package com.festapp.dashboard.equipment.entity;

import com.festapp.dashboard.dashboard.entity.Dashboard;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "equipment",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_equipment_dashboard_name", columnNames = {"dashboard_id", "equipment_name"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "equipmentId")
@ToString(exclude = "dashboard")
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "equipment_id")
    private Long equipmentId;

    @Column(name = "equipment_name", nullable = false, length = 255)
    private String equipmentName;

    @Column(name = "field", length = 255)
    private String field;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dashboard_id", nullable = false)
    private Dashboard dashboard;
}
