package joomidang.papersummary.member.controller.response;

import joomidang.papersummary.common.controller.response.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberSuccessCode implements SuccessCode {
    PROFILE_CREATED("MEM-0001", "프로필 등록 완료"),
    MEMBER_INFO("MEM-0002", "유저 정보 조회");

    private final String value;
    private final String message;
}