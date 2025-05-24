package joomidang.papersummary.comment.controller.request;

import jakarta.validation.constraints.NotBlank;

public record ReplyCreateRequest(
        @NotBlank(message = "답글 내용은 필수입니다.")
        String content
) {
}
