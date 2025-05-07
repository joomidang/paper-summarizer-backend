package joomidang.papersummary.summary.controller.response;

import java.time.LocalDateTime;
import joomidang.papersummary.summary.entity.PublishStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 요약본 편집 완료 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
public class SummaryEditResponse {
    private Long summaryId;
    private PublishStatus status;
    private String markdownUrl;
    private LocalDateTime savedAt;

    /**
     * 요약본 편집 완료 응답 DTO 생성 팩토리 메서드
     *
     * @param summaryId   요약본 ID
     * @param status      발행 상태
     * @param markdownUrl 마크다운 URL
     * @param savedAt     저장 시간
     * @return 요약본 편집 완료 응답 DTO
     */
    public static SummaryEditResponse of(Long summaryId, PublishStatus status, String markdownUrl,
                                         LocalDateTime savedAt) {
        return SummaryEditResponse.builder()
                .summaryId(summaryId)
                .status(status)
                .markdownUrl(markdownUrl)
                .savedAt(savedAt)
                .build();
    }
}