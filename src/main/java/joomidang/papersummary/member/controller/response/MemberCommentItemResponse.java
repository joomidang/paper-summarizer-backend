package joomidang.papersummary.member.controller.response;

import java.time.LocalDateTime;
import joomidang.papersummary.comment.entity.Comment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberCommentItemResponse {
    private Long commentId;
    private Long summaryId;
    private String summaryTitle;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MemberCommentItemResponse from(Comment comment) {
        return MemberCommentItemResponse.builder()
                .commentId(comment.getId())
                .summaryId(comment.getSummary().getId())
                .summaryTitle(comment.getSummary().getTitle())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}