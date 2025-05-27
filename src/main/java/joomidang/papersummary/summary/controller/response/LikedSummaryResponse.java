package joomidang.papersummary.summary.controller.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Arrays;
import joomidang.papersummary.summary.entity.Summary;
import joomidang.papersummary.summary.entity.SummaryLike;

public record LikedSummaryResponse(Long summaryId,
                                   String title,
                                   AuthorResponse author,
                                   LocalDateTime createdAt,
                                   LocalDateTime updatedAt,
                                   Integer likes,
                                   List<String> tags) {
    public static LikedSummaryResponse from(SummaryLike summaryLike) {
        Summary summary = summaryLike.getSummary();

        AuthorResponse author = AuthorResponse.from(summary.getMember());

//      태그 구현 하기!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        List<String> tags = Arrays.asList("태그 구현 아직 안댐;;");

        return new LikedSummaryResponse(
                summary.getId(),
                summary.getTitle(),
                author,
                summaryLike.getCreatedAt(),
                summary.getUpdatedAt(),
                summary.getLikeCount(),
                tags
        );
    }
}
