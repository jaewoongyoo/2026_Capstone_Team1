// 헬스 체크 제어자 - 서버 상태 확인 API
package com.festapp.dashboard.common.controller;

import com.festapp.dashboard.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 헬스 체크 컨트롤러
 *
 * 서버의 상태를 확인하는 공개 API입니다.
 * 배포, 모니터링, 로드 밸런싱 등에서 사용됩니다.
 *
 * @author DashBoar Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/public")
@Tag(name = "Public", description = "공개 API")
public class HealthController {

    @Value("${app.name:DashBoar}")
    private String appName;

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @GetMapping("/health")
    @Operation(summary = "헬스 체크", description = "서버 상태 확인 - 기본 헬스 체크")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        log.debug("Health check requested");
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("timestamp", System.currentTimeMillis());
        data.put("service", appName);

        log.info("헬스 체크 완료: 서버 정상 운영 중");
        return ResponseEntity.ok(ApiResponse.success("서버가 정상 운영 중입니다", data));
    }

    @GetMapping("/health/detailed")
    @Operation(summary = "상세 헬스 체크", description = "서버의 상세한 상태 정보 조회")
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthDetailed() {
        log.debug("Detailed health check requested");
        Map<String, Object> data = new HashMap<>();

        // 시스템 정보
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / (1024 * 1024); // MB
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        long usedMemory = totalMemory - freeMemory;

        // 응답 구성
        data.put("status", "UP");
        data.put("timestamp", LocalDateTime.now());
        data.put("service", appName);
        data.put("version", appVersion);

        // 메모리 정보
        Map<String, Object> memory = new HashMap<>();
        memory.put("maxMemory", maxMemory + "MB");
        memory.put("totalMemory", totalMemory + "MB");
        memory.put("usedMemory", usedMemory + "MB");
        memory.put("freeMemory", freeMemory + "MB");
        memory.put("usagePercentage", String.format("%.2f%%", (usedMemory * 100.0) / maxMemory));
        data.put("memory", memory);

        // 시스템 정보
        Map<String, Object> system = new HashMap<>();
        system.put("osName", System.getProperty("os.name"));
        system.put("osVersion", System.getProperty("os.version"));
        system.put("osArch", System.getProperty("os.arch"));
        system.put("javaVersion", System.getProperty("java.version"));
        system.put("javaVendor", System.getProperty("java.vendor"));
        data.put("system", system);

        // 실행 정보
        Map<String, Object> runtime_info = new HashMap<>();
        runtime_info.put("uptime", ManagementFactory.getRuntimeMXBean().getUptime() + "ms");
        runtime_info.put("activeThreadCount", Thread.activeCount());
        data.put("runtime", runtime_info);

        log.info("상세 헬스 체크 완료");
        return ResponseEntity.ok(ApiResponse.success("서버 상세 정보 조회 성공", data));
    }

    @GetMapping("/health/ready")
    @Operation(summary = "준비 상태 확인", description = "서버가 요청을 처리할 준비가 되었는지 확인 (Kubernetes 준비 탐침)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ready() {
        log.debug("Readiness check requested");
        Map<String, Object> data = new HashMap<>();
        data.put("ready", true);
        data.put("timestamp", LocalDateTime.now());

        log.info("준비 상태 확인 완료: 서버 요청 처리 준비 완료");
        return ResponseEntity.ok(ApiResponse.success("서버 준비 완료", data));
    }

    @GetMapping("/health/live")
    @Operation(summary = "생존 상태 확인", description = "서버가 살아있는지 확인 (Kubernetes 생존 탐침)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> live() {
        log.debug("Liveness check requested");
        Map<String, Object> data = new HashMap<>();
        data.put("alive", true);
        data.put("timestamp", LocalDateTime.now());

        log.info("생존 상태 확인 완료: 서버 정상 운영 중");
        return ResponseEntity.ok(ApiResponse.success("서버 생존 확인", data));
    }
}



