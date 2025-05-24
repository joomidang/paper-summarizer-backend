package joomidang.papersummary.comment.controller.response;

import joomidang.papersummary.common.controller.response.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommentSuccessCode implements SuccessCode {
    COMMENT_CREATED("COM-0001", "댓글이 성공적으로 작성되었습니다."),
    REPLY_CREATED("COM-0002", "답글이 성공적으로 작성되었습니다."),
    COMMENT_UPDATED("COM-0003", "댓글이 성공적으로 수정되었습니다."),
    COMMENT_DELETED("COM-0004", "댓글이 성공적으로 삭제되었습니다."),
    COMMENT_FETCHED("COM-0005", "댓글 정보를 성공적으로 조회했습니다."),
    COMMENTS_FETCHED("COM-0006", "댓글 목록을 성공적으로 조회했습니다.");

    private final String value;
    private final String message;
}
