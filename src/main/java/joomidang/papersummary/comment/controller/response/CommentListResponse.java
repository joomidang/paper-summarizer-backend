package joomidang.papersummary.comment.controller.response;

import java.util.List;
import java.util.stream.Collectors;
import joomidang.papersummary.comment.entity.Comment;
import lombok.Builder;
import org.springframework.data.domain.Page;

@Builder
public record CommentListResponse(List<CommentResponse> comments,
                                  int currentPage,
                                  int totalPages,
                                  long totalElements,
                                  boolean hasNext,
                                  boolean hasPrevious) {
    public static CommentListResponse from(Page<Comment> commentsPage) {
        List<CommentResponse> comments = commentsPage.getContent().stream()
                .map(CommentResponse::from)
                .collect(Collectors.toList());

        return CommentListResponse.builder()
                .comments(comments)
                .currentPage(commentsPage.getNumber())
                .totalPages(commentsPage.getTotalPages())
                .totalElements(commentsPage.getTotalElements())
                .hasNext(commentsPage.hasNext())
                .hasPrevious(commentsPage.hasPrevious())
                .build();
    }
}
