package joomidang.papersummary.comment.exception;

import joomidang.papersummary.comment.controller.response.CommentErrorCode;
import lombok.Getter;

@Getter
public class DeletedCommentException extends RuntimeException {
    private final CommentErrorCode errorCode;

    public DeletedCommentException() {
        super("삭제된 댓글입니다.");
        this.errorCode = CommentErrorCode.DELETED_COMMENT_ACCESS;
    }
}
