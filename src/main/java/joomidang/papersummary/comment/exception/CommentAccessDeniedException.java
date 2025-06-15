package joomidang.papersummary.comment.exception;

import joomidang.papersummary.comment.controller.response.CommentErrorCode;
import lombok.Getter;

@Getter
public class CommentAccessDeniedException extends RuntimeException {
    private final CommentErrorCode errorCode;

    public CommentAccessDeniedException() {
        super("해당 댓글에 접근할 권한이 없습니다.");
        this.errorCode = CommentErrorCode.COMMENT_ACCESS_DENIED;
    }
}
