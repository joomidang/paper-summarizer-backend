package joomidang.papersummary.summary.controller.response;

import joomidang.papersummary.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public enum SummaryErrorCode implements ErrorCode {
    SUMMARY_NOT_FOUND("SUE-0001"),
    SUMMARY_ALREADY_EXISTS("SUE-0002"),
    UNAUTHORIZED_ACCESS("SUE-0003"),
    SUMMARY_CREATION_FAILED("SUE-0004");

    private final String value;
}