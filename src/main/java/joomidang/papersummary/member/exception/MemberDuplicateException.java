package joomidang.papersummary.member.exception;

import joomidang.papersummary.member.controller.response.MemberErrorCode;
import lombok.Getter;

@Getter
public class MemberDuplicateException extends RuntimeException {
    private final MemberErrorCode errorCode;

    public MemberDuplicateException(String message) {

        super(message);
        this.errorCode = MemberErrorCode.DUPLICATE_USERNAME;
    }
}
