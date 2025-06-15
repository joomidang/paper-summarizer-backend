package joomidang.papersummary.summary.controller.request;

import java.util.List;

/**
 * 요약본 편집 요청 DTO
 */
public record SummaryEditRequest(
        String title,
        String brief,
        String markdownContent,
        List<String> tags) {
}
