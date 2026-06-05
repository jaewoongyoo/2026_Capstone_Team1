// Swagger/OpenAPI 설정 - API 문서화 설정
package com.festapp.dashboard.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Swagger/OpenAPI 설정
 *
 * API 문서화를 위한 OpenAPI 설정입니다.
 * 프론트엔드 개발자가 쉽게 이해할 수 있도록 명확한 정보와 예제를 제공합니다.
 *
 * @author DashBoar Team
 * @version 1.0.0
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(createComponents())
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
                .info(createInfo());
    }

    /**
     * API 정보 설정
     */
    private Info createInfo() {
        return new Info()
                .title("DashBoard API")
                .description("대시보드 API\n\n" +
                        "## 프론트에서 보는 추천 흐름\n\n" +
                        "1. **회원가입/로그인**\n" +
                        "- `POST /api/auth/signup`\n" +
                        "- `POST /api/auth/login`\n" +
                        "- 로그인 후 accessToken을 Authorization 헤더에 넣어 이후 API를 호출합니다.\n\n" +
                        "2. **대시보드 진입**\n" +
                        "- `GET /api/dashboards`\n" +
                        "- 사용자 홈/사이드바에서 대시보드 목록을 그릴 때 사용합니다.\n\n" +
                        "3. **대시보드 상세 화면 렌더링**\n" +
                        "- `GET /api/dashboards/{dashboardId}`\n" +
                        "- `GET /api/dashboards/{dashboardId}/widgets`\n" +
                        "- 선택한 대시보드의 메타정보와 위젯 레이아웃을 불러올 때 사용합니다.\n\n" +
                        "4. **ERD 기준 리소스 관리**\n" +
                        "- 대시보드 아래에 장비(`equipment`)가 있고, 장비 아래에 센서(`sensor`)가 있습니다.\n" +
                        "- 위젯은 대시보드에 배치되며, 필요 시 장비/센서를 참조합니다.\n\n" +
                        "5. **시계열 데이터 표시**\n" +
                        "- `GET /api/sensors/{sensorId}/history/numeric`\n" +
                        "- `GET /api/sensors/{sensorId}/history/string`\n" +
                        "- 차트/상태 패널에서 최근 이력을 읽어올 때 사용합니다.\n\n" +
                        "## 프론트가 특히 알아야 할 점\n\n" +
                        "- `equipmentEntityId`, `sensorEntityId`는 **새 ERD 기준 PK**입니다.\n" +
                        "- `equipmentId`, `sensorId` 문자열 필드는 **레거시 호환용 식별자**입니다.\n" +
                        "- 새 화면/신규 기능은 가능하면 `equipmentEntityId`, `sensorEntityId`를 기준으로 구현하는 것을 권장합니다.\n\n" +
                        "## 에러 처리\n\n" +
                        "모든 오류 응답은 아래 형식을 따릅니다:\n" +
                        "```json\n" +
                        "{\n" +
                        "  \"success\": false,\n" +
                        "  \"message\": \"오류 메시지\",\n" +
                        "  \"errorCode\": 4001,\n" +
                        "  \"errorDetail\": \"상세 오류 정보\",\n" +
                        "  \"statusCode\": 401,\n" +
                        "  \"timestamp\": \"2026-04-03T10:30:00\",\n" +
                        "  \"path\": \"/api/auth/login\"\n" +
                        "}\n" +
                        "```\n\n" +
                        "### 주요 에러 코드\n" +
                        "- **4001**: 잘못된 username 또는 password\n" +
                        "- **4002**: 사용자를 찾을 수 없음\n" +
                        "- **4007**: 인증이 필요함\n" +
                        "- **4008**: 권한이 없음 (INSUFFICIENT_PERMISSION)\n" +
                        "- **4100**: 입력값이 유효하지 않음\n" +
                        "- **4201**: 위젯을 찾을 수 없음\n" +
                        "- **4203**: 위젯에 접근할 권한이 없음 (다른 사용자의 위젯)\n\n" +
                        "## 인증\n\n" +
                        "JWT 토큰을 사용합니다. 로그인 후 받은 accessToken을 Authorization 헤더에 포함하세요:\n" +
                        "```\n" +
                        "Authorization: Bearer {accessToken}\n" +
                        "```\n")
                .version("1.0.0")
                .contact(new Contact()
                        .name("DashBoar Team")
                        .url("https://example.com")
                        .email("support@dashboar.com"))
                .license(new License()
                        .name("Apache 2.0")
                        .url("https://www.apache.org/licenses/LICENSE-2.0.html"));
    }

    /**
     * 컴포넌트 설정 (보안, 응답 등)
     */
    private Components createComponents() {
        Components components = new Components();

        // JWT 보안 설정
        components.addSecuritySchemes("bearer-jwt", new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT 인증 토큰\n\n" +
                        "로그인 후 받은 accessToken을 사용합니다.\n" +
                        "형식: Bearer {token}"));

        // 표준 오류 응답 정의
        createErrorResponses(components);

        return components;
    }

    /**
     * 표준 오류 응답 정의
     *
     * Swagger에서 일반적인 오류 응답을 재사용할 수 있도록 정의합니다.
     */
    private void createErrorResponses(Components components) {
        // 401 Unauthorized 응답
        components.addResponses("401-Unauthorized", new ApiResponse()
                .description("인증 실패 또는 토큰 만료")
                .content(new Content()
                        .addMediaType("application/json", new MediaType()
                                .addExamples("응답 예시", new Example()
                                        .value(createErrorExample(
                                                false,
                                                "인증이 필요합니다",
                                                4007,
                                                null,
                                                401))))));

        // 400 Bad Request 응답
        components.addResponses("400-BadRequest", new ApiResponse()
                .description("입력값 검증 실패")
                .content(new Content()
                        .addMediaType("application/json", new MediaType()
                                .addExamples("응답 예시", new Example()
                                        .value(createErrorExample(
                                                false,
                                                "입력값 검증 실패",
                                                4100,
                                                "email: 유효한 이메일 형식이어야 합니다",
                                                400))))));

        // 403 Forbidden 응답
        components.addResponses("403-Forbidden", new ApiResponse()
                .description("접근 권한이 없습니다")
                .content(new Content()
                        .addMediaType("application/json", new MediaType()
                                .addExamples("응답 예시", new Example()
                                        .value(createErrorExample(
                                                false,
                                                "이 위젯에 접근할 권한이 없습니다",
                                                4203,
                                                "widgetId: 123",
                                                403))))));

        // 404 Not Found 응답
        components.addResponses("404-NotFound", new ApiResponse()
                .description("요청한 리소스를 찾을 수 없습니다")
                .content(new Content()
                        .addMediaType("application/json", new MediaType()
                                .addExamples("응답 예시", new Example()
                                        .value(createErrorExample(
                                                false,
                                                "위젯을 찾을 수 없습니다",
                                                4201,
                                                "widgetId: 999",
                                                404))))));

        // 500 Internal Server Error 응답
        components.addResponses("500-InternalServerError", new ApiResponse()
                .description("서버 오류가 발생했습니다")
                .content(new Content()
                        .addMediaType("application/json", new MediaType()
                                .addExamples("응답 예시", new Example()
                                        .value(createErrorExample(
                                                false,
                                                "서버 오류가 발생했습니다",
                                                5000,
                                                "데이터베이스 연결 실패",
                                                500))))));
    }

    /**
     * 오류 응답 예시 생성
     */
    private Map<String, Object> createErrorExample(boolean success, String message, int errorCode, String errorDetail, int statusCode) {
        Map<String, Object> example = new HashMap<>();
        example.put("success", success);
        example.put("message", message);
        example.put("errorCode", errorCode);
        if (errorDetail != null) {
            example.put("errorDetail", errorDetail);
        }
        example.put("statusCode", statusCode);
        example.put("timestamp", "2026-04-03T10:30:00");
        example.put("path", "/api/endpoint");
        return example;
    }
}

