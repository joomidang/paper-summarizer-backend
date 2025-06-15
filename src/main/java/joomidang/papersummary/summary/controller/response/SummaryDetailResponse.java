package joomidang.papersummary.summary.controller.response;

import java.time.LocalDateTime;
import java.util.List;
import joomidang.papersummary.summary.entity.Summary;

public record SummaryDetailResponse(
        Long summaryId,
        String title,
        String brief,
        String markdownUrl,
        String authorName,
        List<String> tags,
        LocalDateTime publishedAt,
        int viewCount,
        int likeCount
) {
    public static SummaryDetailResponse from(Summary summary, String markdownUrl, List<String> tags) {
        return new SummaryDetailResponse(
                summary.getId(),
                summary.getTitle(),
                summary.getBrief(),
                markdownUrl,
                summary.getMember().getName(),
                tags,
                summary.getUpdatedAt(),
                summary.getViewCount(),
                summary.getLikeCount()
        );
    }
}
