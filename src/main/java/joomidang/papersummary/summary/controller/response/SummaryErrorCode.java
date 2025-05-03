package joomidang.papersummary.summary.controller.response;

import joomidang.papersummary.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public enum SummaryErrorCode implements ErrorCode {
    SUMMARY_ALREADY_EXISTS("SUE-0001");

    private final String value;
}
