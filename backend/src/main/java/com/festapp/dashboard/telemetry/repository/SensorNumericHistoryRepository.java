package com.festapp.dashboard.telemetry.repository;

import com.festapp.dashboard.telemetry.entity.SensorNumericHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SensorNumericHistoryRepository extends JpaRepository<SensorNumericHistory, Long> {

    List<SensorNumericHistory> findTop100BySensorSensorIdOrderByTimestampDesc(Long sensorId);

    List<SensorNumericHistory> findBySensorSensorIdAndTimestampBetweenOrderByTimestampDesc(
            Long sensorId,
            LocalDateTime from,
            LocalDateTime to
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM SensorNumericHistory h WHERE h.sensor.sensorId = :sensorId")
    void deleteBySensorId(@Param("sensorId") Long sensorId);
}
