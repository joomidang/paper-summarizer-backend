package joomidang.papersummary.debug.controller.response;

import joomidang.papersummary.common.controller.response.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DebugSuccessCode implements SuccessCode {
    DEBUG_RESPONSE_FETCHED("DEB-0001", "디버깅용 더미 응답을 성공적으로 조회했습니다."),
    CUSTOM_DEBUG_RESPONSE_FETCHED("DEB-0003", "커스터마이징된 디버깅용 더미 응답을 성공적으로 조회했습니다.");

    private final String value;
    private final String message;
}
