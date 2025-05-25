package joomidang.papersummary.member.controller.response;

import java.util.List;
import java.util.stream.Collectors;
import joomidang.papersummary.comment.entity.Comment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberCommentResponse {
    private List<MemberCommentItemResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public static MemberCommentResponse from(Page<Comment> commentPage) {
        List<MemberCommentItemResponse> content = commentPage.getContent().stream()
                .map(MemberCommentItemResponse::from)
                .collect(Collectors.toList());

        return MemberCommentResponse.builder()
                .content(content)
                .page(commentPage.getNumber() + 1) // Page is 0-based, but we want 1-based for the response
                .size(commentPage.getSize())
                .totalElements(commentPage.getTotalElements())
                .totalPages(commentPage.getTotalPages())
                .build();
    }
}