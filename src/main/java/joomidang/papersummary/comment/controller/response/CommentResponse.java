package joomidang.papersummary.comment.controller.response;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import joomidang.papersummary.comment.entity.Comment;
import lombok.Builder;

@Builder
public record CommentResponse(Long id,
                              String content,
                              LocalDateTime createdAt,
                              LocalDateTime updatedAt,
                              Integer likeCount,
                              CommentAuthorResponse author,
                              Long parentId,
                              List<CommentResponse> children) {

    public static CommentResponse from(Comment comment) {
        List<Comment> children = comment.getChildren();
        List<CommentResponse> childrenResponse = children != null ? children.stream()
                .filter(child -> !child.isDeleted())
                .map(CommentResponse::from)
                .collect(Collectors.toList()) : Collections.emptyList();
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .likeCount(comment.getLikeCount())
                .author(CommentAuthorResponse.from(comment.getMember()))
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .children(childrenResponse)
                .build();
    }
}
