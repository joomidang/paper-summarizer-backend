package joomidang.papersummary.summary.controller.response;

import java.time.LocalDateTime;
import joomidang.papersummary.summary.entity.Summary;

public record SummaryResponse(
        Long summaryId,
        String title,
        String brief,
        String authorName,
        String authorProfileImage,
        LocalDateTime createdAt,
        LocalDateTime publishedAt,
        Integer viewCount,
        Integer likeCount,
        Integer commentCount,
        Double popularityScore
) {
    public static SummaryResponse from(Summary summary, Double popularityScore) {
        return new SummaryResponse(
                summary.getId(),
                summary.getTitle(),
                summary.getBrief(),
                summary.getMember().getName(),
                summary.getMember().getProfileImage(),
                summary.getCreatedAt(),
                summary.getUpdatedAt(),
                summary.getViewCount(),
                summary.getLikeCount(),
                summary.getCommentCount(),
                popularityScore
        );
    }
}
