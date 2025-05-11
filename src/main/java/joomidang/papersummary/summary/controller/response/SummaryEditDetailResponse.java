package joomidang.papersummary.summary.controller.response;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import joomidang.papersummary.summary.entity.PublishStatus;
import joomidang.papersummary.summary.entity.Summary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 요약본 편집을 위한 상세 정보 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
public class SummaryEditDetailResponse {
    private Long summaryId;
    private String title;
    private String brief;
    private String markdownUrl;
    private List<String> figures;
    private List<String> tables;
    private List<String> tags;
    private LocalDateTime publishedAt;
    private PublishStatus publishStatus;

    /**
     * Summary 엔티티로부터 응답 DTO를 생성하는 팩토리 메서드
     *
     * @param summary     요약본 엔티티
     * @param markdownUrl 마크다운 URL
     * @param figures     그림 URL 목록
     * @param tables      표 URL 목록
     * @param tags        태그 목록
     * @return 요약본 편집 상세 응답 DTO
     */
    public static SummaryEditDetailResponse from(Summary summary, String markdownUrl,
                                                 List<String> figures, List<String> tables, List<String> tags) {
        return SummaryEditDetailResponse.builder()
                .summaryId(summary.getSummaryId())
                .title(summary.getTitle())
                .brief(summary.getBrief())
                .markdownUrl(markdownUrl)
                .figures(figures)
                .tables(tables)
                .tags(tags)
                .publishedAt(summary.getUpdatedAt())
                .publishStatus(summary.getPublishStatus())
                .build();
    }

    /**
     * 이전 버전과의 호환성을 위한 팩토리 메서드
     */
    public static SummaryEditDetailResponse from(Summary summary, String markdownUrl, List<String> tags) {
        return from(summary, markdownUrl, Collections.emptyList(), Collections.emptyList(), tags);
    }
}
