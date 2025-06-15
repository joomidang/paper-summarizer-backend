package joomidang.papersummary.comment.exception;

import joomidang.papersummary.comment.controller.response.CommentErrorCode;
import lombok.Getter;

@Getter
public class InvalidParentCommentException extends RuntimeException {
    private final CommentErrorCode errorCode;

    public InvalidParentCommentException(Long commentId) {
        super("유효하지 않은 부모 댓글입니다: commentId=" + commentId + "");
        this.errorCode = CommentErrorCode.INVALID_PARENT_COMMENT;
    }
}
