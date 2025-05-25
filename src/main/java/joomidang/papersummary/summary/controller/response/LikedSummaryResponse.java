package joomidang.papersummary.summary.controller.response;

import java.time.LocalDateTime;
import joomidang.papersummary.summary.entity.Summary;
import joomidang.papersummary.summary.entity.SummaryLike;

public record LikedSummaryResponse(Long summaryId,
                                   String title,
                                   String brief,
                                   String authorName,
                                   LocalDateTime likedAt,
                                   LocalDateTime publishedAt,
                                   Integer viewCount,
                                   Integer likeCount,
                                   Integer commentCount) {
    public static LikedSummaryResponse from(SummaryLike summaryLike) {
        Summary summary = summaryLike.getSummary();
        return new LikedSummaryResponse(
                summary.getId(),
                summary.getTitle(),
                summary.getBrief(),
                summary.getMember().getName(),
                summaryLike.getCreatedAt(),
                summary.getUpdatedAt(),
                summary.getViewCount(),
                summary.getLikeCount(),
                summary.getCommentCount()
        );
    }
}
