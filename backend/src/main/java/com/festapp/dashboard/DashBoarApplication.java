// Dashboard 애플리케이션 메인 클래스 - Spring Boot 애플리케이션의 진입점
package com.festapp.dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class DashBoarApplication {

    public static void main(String[] args) {
        SpringApplication.run(DashBoarApplication.class, args);
    }

}
