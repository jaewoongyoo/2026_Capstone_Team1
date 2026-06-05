// 페이징 응답 DTO - 페이징된 데이터를 반환할 때 사용
package com.festapp.dashboard.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 페이징된 데이터를 반환하기 위한 DTO
 *
 * Spring Data의 Page 객체를 표준화된 형식으로 변환합니다.
 *
 * @param <T> 데이터 요소의 타입
 * @author DashBoar Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "페이징된 응답 객체")
public class PaginationResponse<T> {

    @Schema(description = "현재 페이지의 데이터 목록")
    private List<T> content;

    @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
    private int pageNumber;

    @Schema(description = "페이지 크기", example = "10")
    private int pageSize;

    @Schema(description = "전체 요소 개수", example = "100")
    private long totalElements;

    @Schema(description = "전체 페이지 수", example = "10")
    private int totalPages;

    @Schema(description = "마지막 페이지 여부", example = "false")
    private boolean isLast;

    @Schema(description = "첫 페이지 여부", example = "true")
    private boolean isFirst;

    @Schema(description = "다음 페이지 존재 여부", example = "true")
    private boolean hasNext;

    @Schema(description = "이전 페이지 존재 여부", example = "false")
    private boolean hasPrevious;

    /**
     * Spring Data Page 객체로부터 PaginationResponse 생성
     *
     * @param page Spring Data Page 객체
     * @param content 현재 페이지의 데이터
     * @return PaginationResponse
     */
    public static <T> PaginationResponse<T> from(Page<?> page, List<T> content) {
        return PaginationResponse.<T>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isLast(page.isLast())
                .isFirst(page.isFirst())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    /**
     * Spring Data Page<T> 객체로부터 PaginationResponse 생성 (매핑 불필요)
     *
     * @param page Spring Data Page 객체
     * @return PaginationResponse
     */
    public static <T> PaginationResponse<T> from(Page<T> page) {
        return PaginationResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isLast(page.isLast())
                .isFirst(page.isFirst())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }
}

