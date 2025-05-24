package joomidang.papersummary.comment.exception;

import joomidang.papersummary.comment.controller.response.CommentErrorCode;
import lombok.Getter;

@Getter
public class InvalidLikeActionException extends RuntimeException {
    private final CommentErrorCode errorCode;

    public InvalidLikeActionException(String action) {
        super("유효하지 않은 액션입니다: " + action + ". 'like' 또는 'dislike'만 허용됩니다.");
        this.errorCode = CommentErrorCode.INVALID_LIKE_ACTION;
    }
}