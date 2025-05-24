package joomidang.papersummary.member.controller.response;

import java.time.LocalDateTime;
import joomidang.papersummary.summary.entity.PublishStatus;
import joomidang.papersummary.summary.entity.Summary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberSummaryItemResponse {
    private Long summaryId;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer likes;
    private Integer commentCount;
    private boolean isPublic;

    public static MemberSummaryItemResponse from(Summary summary) {
        return MemberSummaryItemResponse.builder()
                .summaryId(summary.getId())
                .title(summary.getTitle())
                .createdAt(summary.getCreatedAt())
                .updatedAt(summary.getUpdatedAt())
                .likes(summary.getLikeCount())
                .commentCount(summary.getCommentCount())
                .isPublic(summary.getPublishStatus() == PublishStatus.PUBLISHED)
                .build();
    }
}