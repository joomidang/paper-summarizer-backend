package joomidang.papersummary.summary.exception;

import joomidang.papersummary.summary.controller.response.SummaryErrorCode;
import lombok.Getter;

@Getter
public class SummaryCreationFailedException extends RuntimeException {
    private final SummaryErrorCode errorCode;

    public SummaryCreationFailedException(String message) {
        super("요약 생성에 실패했습니다: " + message);
        this.errorCode = SummaryErrorCode.SUMMARY_CREATION_FAILED;
    }
}