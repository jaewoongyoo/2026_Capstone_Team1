package com.festapp.dashboard.dashboard.widget.repository;

import com.festapp.dashboard.dashboard.widget.entity.DashboardWidget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DashboardWidgetRepository extends JpaRepository<DashboardWidget, Long> {

    List<DashboardWidget> findByDashboardUserUserIdOrderByWidgetIdAsc(Long userId);

    List<DashboardWidget> findByDashboardDashboardIdOrderByWidgetIdAsc(Long dashboardId);

    List<DashboardWidget> findByDashboardUserUserIdAndEquipmentEquipmentNameOrderByWidgetIdAsc(Long userId, String equipmentName);

    List<DashboardWidget> findByDashboardUserUserIdAndEquipmentEquipmentIdOrderByWidgetIdAsc(Long userId, Long equipmentId);

    Optional<DashboardWidget> findByWidgetIdAndDashboardUserUserId(Long widgetId, Long userId);

    @Query("SELECT COUNT(w) FROM DashboardWidget w WHERE w.dashboard.user.userId = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM DashboardWidget w WHERE w.dashboard.user.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(w) FROM DashboardWidget w WHERE w.dashboard.user.userId = :userId AND w.equipment.equipmentName = :equipmentName")
    long countByUserIdAndEquipmentId(@Param("userId") Long userId, @Param("equipmentName") String equipmentName);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE DashboardWidget w SET w.sensor = null WHERE w.sensor.sensorId = :sensorId")
    void clearSensorReference(@Param("sensorId") Long sensorId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE DashboardWidget w SET w.sensor = null, w.equipment = null WHERE w.equipment.equipmentId = :equipmentId")
    void clearEquipmentAndSensorReferences(@Param("equipmentId") Long equipmentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE FROM DashboardWidget w
            WHERE w.dashboard.user.userId = :userId
              AND w.equipment.equipmentId = :equipmentId
            """)
    void deleteByUserIdAndEquipmentId(@Param("userId") Long userId, @Param("equipmentId") Long equipmentId);
}
