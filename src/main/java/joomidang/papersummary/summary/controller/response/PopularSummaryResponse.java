package joomidang.papersummary.summary.controller.response;

import java.time.LocalDateTime;
import joomidang.papersummary.summary.entity.Summary;

public record PopularSummaryResponse(
        Long summaryId,
        String title,
        String brief,
        String authorName,
        String authorProfileImage,
        LocalDateTime publishedAt,
        Integer viewCount,
        Integer likeCount,
        Integer commentCount,
        Double popularityScore
) {
    public static PopularSummaryResponse from(Summary summary, Double popularityScore) {
        return new PopularSummaryResponse(
                summary.getId(),
                summary.getTitle(),
                summary.getBrief(),
                summary.getMember().getName(),
                summary.getMember().getProfileImage(),
                summary.getUpdatedAt(),
                summary.getViewCount(),
                summary.getLikeCount(),
                summary.getCommentCount(),
                popularityScore
        );
    }
}
