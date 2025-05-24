package joomidang.papersummary.comment.controller.response;

import joomidang.papersummary.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommentErrorCode implements ErrorCode {
    COMMENT_NOT_FOUND("COM-E001"),
    COMMENT_ACCESS_DENIED("COM-E002"),
    INVALID_COMMENT_CONTENT("COM-E003"),
    DELETED_COMMENT_ACCESS("COM-E004"),
    INVALID_PARENT_COMMENT("COM-E005");

    private final String value;
}
