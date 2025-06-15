package joomidang.papersummary.comment.controller.request;

import jakarta.validation.constraints.NotBlank;

public record CommentUpdateRequest(
        @NotBlank(message = "댓글 내용은 필수입니다.")
        String content
) {
}
