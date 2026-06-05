package com.festapp.dashboard.telemetry.repository;

import com.festapp.dashboard.telemetry.entity.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SensorRepository extends JpaRepository<Sensor, Long> {

    Optional<Sensor> findBySensorIdAndEquipmentEquipmentId(Long sensorId, Long equipmentId);

    Optional<Sensor> findBySensorIdAndEquipmentDashboardUserUserId(Long sensorId, Long userId);

    Optional<Sensor> findBySensorNameAndEquipmentEquipmentId(String sensorName, Long equipmentId);

    List<Sensor> findByEquipmentEquipmentIdOrderBySensorIdAsc(Long equipmentId);

    @Query("""
            SELECT s
            FROM Sensor s
            WHERE s.equipment.dashboard.user.userId = :userId
              AND LOWER(s.sensorName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY s.sensorId ASC
            """)
    List<Sensor> searchByUserAndKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);

    @Query("""
            SELECT s
            FROM Sensor s
            WHERE s.equipment.equipmentId = :equipmentId
              AND s.equipment.dashboard.user.userId = :userId
              AND LOWER(s.sensorName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY s.sensorId ASC
            """)
    List<Sensor> searchByEquipmentAndKeyword(
            @Param("equipmentId") Long equipmentId,
            @Param("userId") Long userId,
            @Param("keyword") String keyword);

    List<Sensor> findByEquipmentDashboardUserUserIdOrderBySensorIdAsc(Long userId);
}
