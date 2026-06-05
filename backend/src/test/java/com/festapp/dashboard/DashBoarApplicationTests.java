// 애플리케이션 통합 테스트 - Spring Boot 애플리케이션 컨텍스트 로드 테스트
package com.festapp.dashboard;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class DashBoarApplicationTests {

    @Test
    void contextLoads() {
    }

}
