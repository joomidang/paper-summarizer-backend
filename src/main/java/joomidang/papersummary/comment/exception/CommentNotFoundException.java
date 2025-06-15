package joomidang.papersummary.comment.exception;

import joomidang.papersummary.comment.controller.response.CommentErrorCode;
import lombok.Getter;

@Getter
public class CommentNotFoundException extends RuntimeException {
    private final CommentErrorCode errorCode;

    public CommentNotFoundException(Long commentId) {
        super("해당 댓글을 찾을 수 없습니다. commentId=" + commentId);
        this.errorCode = CommentErrorCode.COMMENT_NOT_FOUND;
    }
}
