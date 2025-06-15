package joomidang.papersummary.comment.exception;

import joomidang.papersummary.comment.controller.response.CommentErrorCode;
import lombok.Getter;

@Getter
public class UnpublishedSummaryCommentException extends RuntimeException {
    private final CommentErrorCode errorCode;

    public UnpublishedSummaryCommentException() {
        super("발행되지 않은 요약본에는 댓글을 작성할 수 없습니다.");
        this.errorCode = CommentErrorCode.UNPUBLISHED_SUMMARY_COMMENT;
    }
}
