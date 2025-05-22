package joomidang.papersummary.member.controller.response;

import joomidang.papersummary.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public enum MemberErrorCode implements ErrorCode {
    MEMBER_NOT_FOUND("MEM-0001"),
    VALIDATION_ERROR("MEM-0002"),
    DUPLICATE_USERNAME("MEM-0003");
    private final String value;
}
