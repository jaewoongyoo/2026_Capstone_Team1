package com.festapp.dashboard.dashboard.repository;

import com.festapp.dashboard.dashboard.entity.Dashboard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DashboardRepository extends JpaRepository<Dashboard, Long> {

    List<Dashboard> findByUserUserIdOrderByDashboardIdAsc(Long userId);

    Optional<Dashboard> findByDashboardIdAndUserUserId(Long dashboardId, Long userId);

    Optional<Dashboard> findFirstByUserUserIdOrderByDashboardIdAsc(Long userId);

    Optional<Dashboard> findByShareToken(String shareToken);
}
