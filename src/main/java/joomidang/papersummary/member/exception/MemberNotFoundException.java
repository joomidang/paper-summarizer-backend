package joomidang.papersummary.member.exception;

import joomidang.papersummary.member.controller.response.MemberErrorCode;
import lombok.Getter;

@Getter
public class MemberNotFoundException extends RuntimeException {
    private final MemberErrorCode errorCode;

    public MemberNotFoundException(String message) {
        super(message);
        this.errorCode = MemberErrorCode.MEMBER_NOT_FOUND;
    }
}
