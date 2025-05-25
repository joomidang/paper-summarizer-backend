package joomidang.papersummary.member.controller.response;

import joomidang.papersummary.common.controller.response.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberSuccessCode implements SuccessCode {
    PROFILE_CREATED("MEM-0001", "프로필 등록 완료"),
    MEMBER_INFO("MEM-0002", "유저 정보 조회"),
    MEMBER_SUMMARIES("MEM-0003", "사용자 작성 요약 목록 조회 성공"),
    PROFILE_UPDATED("MEM-0004", "프로필 수정 완료"),
    MEMBER_LIKED_SUMMARIES("MEM-0005", "좋아요한 요약 목록 조회 성공"),
    MEMBER_COMMENTS("MEM-0006", "사용자 댓글 목록 조회 성공");


    private final String value;
    private final String message;
}
