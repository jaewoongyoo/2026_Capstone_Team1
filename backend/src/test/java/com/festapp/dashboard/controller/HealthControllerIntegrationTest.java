// 헬스 체크 컨트롤러 통합 테스트
package com.festapp.dashboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.festapp.dashboard.config.IntegrationTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Import(IntegrationTestConfig.class)
@DisplayName("HealthController 통합 테스트")
public class HealthControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .build();
    }

    @Test
    @DisplayName("GET /api/public/health - 기본 헬스 체크")
    void testBasicHealthCheck() throws Exception {
        mockMvc.perform(get("/api/public/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("서버가 정상 운영 중입니다"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.service").exists())
                .andExpect(jsonPath("$.data.timestamp").exists())
                .andExpect(jsonPath("$.statusCode").value(200));
    }

    @Test
    @DisplayName("GET /api/public/health/detailed - 상세 헬스 체크")
    void testDetailedHealthCheck() throws Exception {
        mockMvc.perform(get("/api/public/health/detailed")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.version").exists())
                .andExpect(jsonPath("$.data.memory").exists())
                .andExpect(jsonPath("$.data.memory.maxMemory").exists())
                .andExpect(jsonPath("$.data.memory.totalMemory").exists())
                .andExpect(jsonPath("$.data.memory.usedMemory").exists())
                .andExpect(jsonPath("$.data.memory.freeMemory").exists())
                .andExpect(jsonPath("$.data.memory.usagePercentage").exists())
                .andExpect(jsonPath("$.data.system").exists())
                .andExpect(jsonPath("$.data.system.osName").exists())
                .andExpect(jsonPath("$.data.system.javaVersion").exists())
                .andExpect(jsonPath("$.data.runtime").exists())
                .andExpect(jsonPath("$.data.runtime.uptime").exists())
                .andExpect(jsonPath("$.data.runtime.activeThreadCount").isNumber());
    }

    @Test
    @DisplayName("GET /api/public/health/ready - 준비 상태 확인")
    void testReadinessCheck() throws Exception {
        mockMvc.perform(get("/api/public/health/ready")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("서버 준비 완료"))
                .andExpect(jsonPath("$.data.ready").value(true))
                .andExpect(jsonPath("$.data.timestamp").exists());
    }

    @Test
    @DisplayName("GET /api/public/health/live - 생존 상태 확인")
    void testLivenessCheck() throws Exception {
        mockMvc.perform(get("/api/public/health/live")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("서버 생존 확인"))
                .andExpect(jsonPath("$.data.alive").value(true))
                .andExpect(jsonPath("$.data.timestamp").exists());
    }

    @Test
    @DisplayName("헬스 체크 엔드포인트는 인증 없이 접근 가능")
    void testHealthCheckNoAuthentication() throws Exception {
        mockMvc.perform(get("/api/public/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/public/health/detailed")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/public/health/ready")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/public/health/live")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}

