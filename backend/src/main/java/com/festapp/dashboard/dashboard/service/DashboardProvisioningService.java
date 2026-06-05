package com.festapp.dashboard.dashboard.service;

import com.festapp.dashboard.dashboard.entity.Dashboard;
import com.festapp.dashboard.dashboard.repository.DashboardRepository;
import com.festapp.dashboard.user.entity.User;
import com.festapp.dashboard.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardProvisioningService {

    private final DashboardRepository dashboardRepository;
    private final UserRepository userRepository;

    @Transactional
    public Dashboard ensureDefaultDashboard(User user) {
        return dashboardRepository.findFirstByUserUserIdOrderByDashboardIdAsc(user.getUserId())
                .orElseGet(() -> dashboardRepository.save(
                        Dashboard.builder()
                                .dashboardName(user.getUsername() + " Dashboard")
                                .description("Default dashboard for " + user.getUsername())
                                .user(user)
                                .build()
                ));
    }

    @Transactional
    public Dashboard getSystemDefaultDashboard() {
        // ADMIN 권한을 가진 첫 번째 사용자 또는 아무 사용자나 찾음
        User defaultUser = userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.Role.ADMIN)
                .findFirst()
                .orElseGet(() -> userRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> new RuntimeException("No users found in the system to assign the default dashboard")));
        return ensureDefaultDashboard(defaultUser);
    }
}
