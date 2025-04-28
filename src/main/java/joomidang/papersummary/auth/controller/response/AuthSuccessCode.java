package joomidang.papersummary.auth.controller.response;

import joomidang.papersummary.common.controller.response.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthSuccessCode implements SuccessCode {
    LOGIN("AUS-0001", "로그인이 완료되었습니다."),
    WITHDRAW("AUS-0002", "회원 탈퇴가 완료되었습니다."),
    TOKEN_REFRESH("AUS-0003", "토큰이 갱신되었습니다.");

    private final String value;
    private final String message;
}
