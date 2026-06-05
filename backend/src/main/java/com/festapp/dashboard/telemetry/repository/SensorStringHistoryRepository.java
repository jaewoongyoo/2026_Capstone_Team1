package com.festapp.dashboard.telemetry.repository;

import com.festapp.dashboard.telemetry.entity.SensorStringHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SensorStringHistoryRepository extends JpaRepository<SensorStringHistory, Long> {

    List<SensorStringHistory> findTop100BySensorSensorIdOrderByTimestampDesc(Long sensorId);

    List<SensorStringHistory> findBySensorSensorIdAndTimestampBetweenOrderByTimestampDesc(
            Long sensorId,
            LocalDateTime from,
            LocalDateTime to
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM SensorStringHistory h WHERE h.sensor.sensorId = :sensorId")
    void deleteBySensorId(@Param("sensorId") Long sensorId);
}
