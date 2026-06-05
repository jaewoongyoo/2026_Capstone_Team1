package com.festapp.dashboard.equipment.repository;

import com.festapp.dashboard.equipment.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    Optional<Equipment> findByEquipmentIdAndDashboardDashboardId(Long equipmentId, Long dashboardId);

    Optional<Equipment> findByEquipmentIdAndDashboardUserUserId(Long equipmentId, Long userId);

    Optional<Equipment> findByEquipmentNameAndDashboardDashboardId(String equipmentName, Long dashboardId);

    List<Equipment> findByEquipmentName(String equipmentName);

    Optional<Equipment> findFirstByEquipmentName(String equipmentName);

    Optional<Equipment> findFirstByEquipmentNameAndDashboardUserUserId(String equipmentName, Long userId);

    List<Equipment> findByDashboardUserUserIdOrderByEquipmentIdAsc(Long userId);

    List<Equipment> findByDashboardDashboardIdOrderByEquipmentIdAsc(Long dashboardId);

    @Query("""
            SELECT e
            FROM Equipment e
            WHERE e.dashboard.user.userId = :userId
              AND (
                    LOWER(e.equipmentName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(COALESCE(e.field, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  )
            ORDER BY e.equipmentId ASC
            """)
    List<Equipment> searchByUserAndKeyword(
            @Param("userId") Long userId,
            @Param("keyword") String keyword);

    @Query("""
            SELECT e
            FROM Equipment e
            WHERE e.dashboard.dashboardId = :dashboardId
              AND e.dashboard.user.userId = :userId
              AND (
                    LOWER(e.equipmentName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(COALESCE(e.field, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  )
            ORDER BY e.equipmentId ASC
            """)
    List<Equipment> searchByDashboardAndKeyword(
            @Param("dashboardId") Long dashboardId,
            @Param("userId") Long userId,
            @Param("keyword") String keyword);
}
