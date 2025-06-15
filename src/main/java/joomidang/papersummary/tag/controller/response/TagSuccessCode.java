package joomidang.papersummary.tag.controller.response;

import joomidang.papersummary.common.controller.response.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TagSuccessCode implements SuccessCode {
    TAG_SUCCESS_CODE("TS-0001", "인기 태그를 성공적으로 조회했습니다");

    private final String value;
    private final String message;
}
