// 테스트 환경 설정
package com.festapp.dashboard.config;

import org.springframework.boot.test.context.TestConfiguration;

/**
 * 통합 테스트 환경 자동 설정
 *
 * @SpringBootTest에서 필요한 빈들을 정의합니다.
 * ObjectMapper는 메인 설정의 JacksonConfig에서 자동으로 로드됩니다.
 */
@TestConfiguration
public class IntegrationTestConfig {

}

